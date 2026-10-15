package com.qamqormed.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qamqormed.entity.DoctorNurseAssignment;
import com.qamqormed.entity.ClinicSpecialty;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.NurseProfile;
import com.qamqormed.entity.Role;
import com.qamqormed.entity.City;
import com.qamqormed.dto.DataIntegrityAuditResponse;
import com.qamqormed.repository.*;
import com.qamqormed.service.AiToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:qamqormed-catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-that-is-at-least-32-characters-long",
        "app.jwt.expiration=7200000"
})
class CatalogSeedIntegrityTest {
    @Autowired RegionRepository regions;
    @Autowired CityRepository cities;
    @Autowired ClinicRepository clinics;
    @Autowired SpecialtyRepository specialties;
    @Autowired ClinicSpecialtyRepository clinicSpecialties;
    @Autowired DoctorProfileRepository doctors;
    @Autowired NurseProfileRepository nurses;
    @Autowired DoctorNurseAssignmentRepository assignments;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired DevDataInitializer initializer;
    @Autowired com.qamqormed.service.DataIntegrityAuditService integrityAudit;
    @Autowired AiToolService aiTools;
    @Autowired ObjectMapper objectMapper;

    @Test
    @Transactional
    void importsCurrentCatalogAndOneToOneDemoAssignments() throws Exception {
        assertEquals(20, regions.count());
        assertEquals(90, cities.count());
        assertEquals(53, specialties.count());
        long activeRealClinics = clinics.findAll().stream().filter(c -> c.isReal() && c.isActive()).count();
        long activeSpecialties = specialties.findAll().stream().filter(s -> s.isActive()).count();
        long expectedPairs = activeRealClinics * activeSpecialties;
        assertEquals(31, activeRealClinics);
        assertTrue(clinics.findAll().stream().filter(c -> c.isReal()).allMatch(c -> c.isActive() && "MOH_OFFICIAL".equals(c.getSource())));
        assertEquals(20, regions.findAll().stream().map(com.qamqormed.entity.Region::getCode).distinct().count());
        assertEquals(90, cities.findAll().stream().map(com.qamqormed.entity.City::getCode).distinct().count());
        assertEquals(expectedPairs, clinicSpecialties.count());
        assertEquals(expectedPairs, doctors.countByDemoTrueAndActiveTrue());
        assertEquals(expectedPairs, nurses.countByDemoTrueAndActiveTrue());
        assertEquals(expectedPairs, assignments.findAll().stream().filter(DoctorNurseAssignment::isActive).count());
        assertEquals(expectedPairs, doctors.findAll().stream().filter(d -> d.isDemo() && d.isActive()).map(DoctorProfile::getSeedKey).distinct().count());
        assertEquals(expectedPairs, nurses.findAll().stream().filter(n -> n.isDemo() && n.isActive()).map(NurseProfile::getSeedKey).distinct().count());
        Set<String> expectedDoctorSeeds = clinics.findAll().stream().filter(c -> c.isReal() && c.isActive())
                .flatMap(clinic -> specialties.findAll().stream().filter(s -> s.isActive()).map(specialty -> "DEMO_DOCTOR_" + clinic.getId() + "_" + specialty.getCode()))
                .collect(Collectors.toSet());
        Set<String> expectedNurseSeeds = expectedDoctorSeeds.stream().map(seed -> seed.replace("DEMO_DOCTOR_", "DEMO_NURSE_")).collect(Collectors.toSet());
        assertEquals(expectedDoctorSeeds, doctors.findAll().stream().filter(d -> d.isDemo() && d.isActive()).map(DoctorProfile::getSeedKey).collect(Collectors.toSet()));
        assertEquals(expectedNurseSeeds, nurses.findAll().stream().filter(n -> n.isDemo() && n.isActive()).map(NurseProfile::getSeedKey).collect(Collectors.toSet()));
        assertTrue(assignments.findAll().stream().filter(DoctorNurseAssignment::isActive).allMatch(a -> a.getDoctor().getClinic().getId().equals(a.getNurse().getClinic().getId())));
        assertTrue(users.findByPhone("+77000000002").isPresent());
        assertEquals(Role.DOCTOR, users.findByPhone("+77000000002").orElseThrow().getRole());
        assertTrue(passwordEncoder.matches("Doctor123!", users.findByPhone("+77000000002").orElseThrow().getPassword()));
        assertTrue(doctors.findAll().stream().filter(d -> d.isDemo() && d.isActive()).allMatch(d -> assignments.findActiveByDoctorId(d.getId()).isPresent()));
        assertTrue(nurses.findAll().stream().filter(n -> n.isDemo() && n.isActive()).allMatch(n -> assignments.findActiveByNurseId(n.getId()).isPresent()));
        assertEquals(expectedPairs, clinicSpecialties.findAll().stream().filter(ClinicSpecialty::isDemo).count());
        assertTrue(clinicSpecialties.findAll().stream().allMatch(link -> link.getClinic().isReal() && link.getClinic().isActive() && link.getSpecialty().isActive()));
        DataIntegrityAuditResponse audit = integrityAudit.audit();
        assertEquals(0, audit.doctorsWithoutClinic() + audit.doctorsWithoutSpecialty() + audit.doctorsWithoutCity() + audit.doctorsWithoutRegion());
        assertEquals(0, audit.nursesWithoutClinic() + audit.nursesWithoutDoctor() + audit.nursesWithClinicMismatch());
        assertEquals(0, audit.clinicsWithoutCity() + audit.citiesWithoutRegion());

        long usersBeforeRerun = users.count();
        initializer.run();
        assertEquals(usersBeforeRerun, users.count());
        assertEquals(expectedPairs, clinicSpecialties.count());
        assertEquals(expectedPairs, doctors.countByDemoTrueAndActiveTrue());
        assertEquals(expectedPairs, nurses.countByDemoTrueAndActiveTrue());
        assertEquals(expectedPairs, assignments.findAll().stream().filter(DoctorNurseAssignment::isActive).count());
    }

    @Test
    void aiAcceptsEveryDatabaseCityNameInAllStoredLanguages() {
        var patient = users.findByPhone("+77000000001").orElseThrow();
        for (City city : cities.findAllPublic()) {
            for (String name : List.of(city.getName(), city.getNameRu(), city.getNameKk(), city.getNameEn()).stream().distinct().toList()) {
                var arguments = objectMapper.createObjectNode().put("city", name);
                AiToolService.AiToolExecutionResult result = aiTools.execute(patient.getId(), Role.PATIENT, "search_doctors", arguments);
                assertTrue(result.success(), city.getName() + " / " + name + ": " + result.message());
            }
        }
    }
}
