package com.qamqormed.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qamqormed.entity.*;
import com.qamqormed.repository.*;
import com.qamqormed.service.StaffProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Idempotent development importer. Catalog data lives in resources/data;
 * personnel are generated algorithmically for every Clinic x Specialty pair.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final String DEMO_PASSWORD = "DemoStaff123!";
    private static final String DEMO_BIO = "DEMO-профиль QamqorMed. Не является сотрудником указанной медицинской организации.";
    private static final String[] FIRST_NAMES = {"Ayan", "Aigerim", "Alikhan", "Aruzhan", "Daniyar", "Dana", "Dias", "Dinara", "Erasyl", "Fariza", "Madi", "Madina", "Nursultan", "Nazym", "Rauan", "Sabina", "Sanzhar", "Saule", "Timur", "Tomiris", "Yerlan", "Zhanar", "Zhandos", "Zhibek"};
    private static final String[] LAST_NAMES = {"Sarsenov", "Tulegenova", "Beketov", "Ospanova", "Kassymov", "Nurpeisova", "Akhmetov", "Serikova", "Zhaksybekov", "Muratova", "Kairatov", "Amanzhanova", "Saparov", "Mukanova", "Kenzhebekov", "Aubakirova", "Yessenov", "Kaliyeva", "Baimukhanov", "Tursynova", "Rakhimov", "Bekmuratova", "Zholdasov", "Iskakova", "Abdullin", "Bekturova", "Kozhabekov", "Orazbayeva", "Seisenov", "Kumarova", "Nurgaliyev", "Aitova"};
    private static final String[] MIDDLE_NAMES = {"Nurlanuly", "Serikkyzy", "Maratuly", "Aidarovna", "Kanatuly", "Bolatkyzy", "Ermekuly", "Dauletovna", "Ruslanuly", "Maksatkyzy", "Almasuly", "Zhanibekovna"};

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regions;
    private final CityRepository cities;
    private final ClinicRepository clinics;
    private final SpecialtyRepository specialties;
    private final ClinicSpecialtyRepository clinicSpecialties;
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final DoctorNurseAssignmentRepository assignments;
    private final AppointmentRepository appointments;
    private final StaffProfileService staff;
    private final JdbcTemplate jdbcTemplate;
    private String demoPasswordHash;

    public DevDataInitializer(UserRepository users, PasswordEncoder passwordEncoder, RegionRepository regions,
                              CityRepository cities, ClinicRepository clinics, SpecialtyRepository specialties,
                              ClinicSpecialtyRepository clinicSpecialties, DoctorProfileRepository doctors,
                              NurseProfileRepository nurses, DoctorNurseAssignmentRepository assignments,
                              AppointmentRepository appointments, StaffProfileService staff,
                              JdbcTemplate jdbcTemplate) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.regions = regions;
        this.cities = cities;
        this.clinics = clinics;
        this.specialties = specialties;
        this.clinicSpecialties = clinicSpecialties;
        this.doctors = doctors;
        this.nurses = nurses;
        this.assignments = assignments;
        this.appointments = appointments;
        this.staff = staff;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        demoPasswordHash = passwordEncoder.encode(DEMO_PASSWORD);
        createUserIfMissing("Demo", "Patient", "+77000000001", "Patient123!", Role.PATIENT);
        createUserIfMissing("Demo", "Doctor", "+77000000002", "Doctor123!", Role.DOCTOR);
        createUserIfMissing("Demo", "Nurse", "+77000000003", "Nurse123!", Role.NURSE);
        createUserIfMissing("Demo", "Admin", "+77000000004", "Admin123!", Role.ADMIN);

        Map<String, Region> regionByCode = importRegions();
        Map<String, City> cityByCode = importCities(regionByCode);
        Map<String, Specialty> specialtyByCode = importSpecialties();
        List<Clinic> sourceClinics = importClinics(cityByCode);
        deactivateLegacyDemoClinic();
        retireLegacyGeneratedStaff();

        List<Specialty> catalogSpecialties = specialtyByCode.values().stream()
                .sorted(Comparator.comparing(Specialty::getCode)).toList();
        List<Clinic> catalogClinics = clinics.findAll().stream()
                .filter(clinic -> clinic.isReal() && clinic.isActive())
                .sorted(Comparator.comparing(Clinic::getId)).toList();

        removeStaleClinicSpecialtyLinks(catalogClinics, catalogSpecialties);
        retireDemoStaffOutsideCatalog(catalogClinics, catalogSpecialties);
        linkAllDemoSpecialties(catalogClinics, catalogSpecialties);
        createAllDemoStaff(catalogClinics, catalogSpecialties);
        ensureAppointmentStatusConstraint();
        seedDemoWorkflowAppointment();
        ValidationReport validation = validateCatalog(catalogClinics, catalogSpecialties);
        if (!validation.success()) {
            throw new IllegalStateException("QamqorMed DEMO catalog validation failed: " + validation);
        }

        reportClinicImport(sourceClinics, cityByCode);
        log.info("QamqorMed catalog ready: regions={}, cities={}, active real clinics={}, specialties={}, clinic-specialty links={}, DEMO doctors={}, DEMO nurses={}, active assignments={}",
                regions.count(), cities.count(), catalogClinics.size(), catalogSpecialties.size(), validation.clinicSpecialtyLinks(),
                validation.demoDoctors(), validation.demoNurses(), validation.assignments());
        log.info("Catalog validation: missing clinic-specialty doctors={}, doctors without nurse={}, nurses without doctor={}, cross-clinic assignments={}, duplicate doctors={}, duplicate nurses={}, duplicate assignments={}",
                validation.missingClinicSpecialtyDoctors(), validation.doctorsWithoutNurse(), validation.nursesWithoutDoctor(),
                validation.crossClinicAssignments(), validation.duplicateDoctors(), validation.duplicateNurses(), validation.duplicateAssignments());
    }

    private void seedDemoWorkflowAppointment() {
        User patient = users.findByPhone("+77000000001").orElse(null);
        User doctorUser = users.findByPhone("+77000000002").orElse(null);
        if (patient == null || doctorUser == null || appointments.existsByBookedByIdAndNote(patient.getId(), "DEV_WORKFLOW_SEED")) return;
        DoctorProfile doctor = doctors.findByUserId(doctorUser.getId()).filter(DoctorProfile::isActive).orElse(null);
        if (doctor == null) return;
        patient.setPreferredRegion(doctor.getClinic().getCity().getRegion());
        patient.setPreferredCity(doctor.getClinic().getCity());
        users.save(patient);
        Appointment appointment = new Appointment();
        appointment.setBookedBy(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNote("DEV_WORKFLOW_SEED");
        appointments.save(appointment);
        log.info("Seeded development workflow appointment {} for Demo Patient -> {}", appointment.getId(), doctorUser.getPhone());
    }

    /**
     * Hibernate update does not reliably replace an old PostgreSQL CHECK constraint.
     * Existing dev databases may still contain the pre-workflow constraint that
     * rejects BOOKED, so repair it before seeding the workflow appointment.
     */
    private void ensureAppointmentStatusConstraint() {
        jdbcTemplate.execute("ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check");
        jdbcTemplate.update("UPDATE appointments SET status = 'BOOKED' WHERE status = 'SCHEDULED'");
        jdbcTemplate.update("UPDATE appointments SET status = 'ARRIVED' WHERE status = 'CHECKED_IN' OR status = 'CONFIRMED'");
        jdbcTemplate.update("UPDATE appointments SET status = 'IN_APPOINTMENT' WHERE status = 'DOCTOR_IN_PROGRESS' OR status = 'IN_PROGRESS'");
        jdbcTemplate.execute("""
                ALTER TABLE appointments ADD CONSTRAINT appointments_status_check
                CHECK (status IN ('BOOKED', 'CONFIRMED', 'ARRIVED', 'PREPARING', 'READY',
                                  'IN_APPOINTMENT', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
                """);
    }

    private Map<String, Region> importRegions() throws IOException {
        Map<String, Region> result = new LinkedHashMap<>();
        for (JsonNode row : read("data/regions.json")) {
            String code = text(row, "code");
            Region region = regions.findByCodeIgnoreCase(code).orElseGet(Region::new);
            region.setCode(code);
            region.setNameRu(text(row, "nameRu"));
            region.setNameKk(text(row, "nameKk"));
            region.setNameEn(text(row, "nameEn"));
            region.setName(region.getNameRu());
            region.setType(text(row, "type"));
            region.setActive(true);
            result.put(code, regions.save(region));
        }
        return result;
    }

    private Map<String, City> importCities(Map<String, Region> regionByCode) throws IOException {
        Map<String, City> result = new LinkedHashMap<>();
        for (JsonNode row : read("data/cities.json")) {
            String code = text(row, "code");
            Region region = require(regionByCode, text(row, "regionCode"));
            City city = cities.findByCodeIgnoreCase(code)
                    .orElseGet(() -> cities.findByNameIgnoreCaseAndRegionId(text(row, "nameRu"), region.getId()).orElseGet(City::new));
            city.setCode(code);
            city.setRegion(region);
            city.setNameRu(text(row, "nameRu"));
            city.setNameKk(text(row, "nameKk"));
            city.setNameEn(text(row, "nameEn"));
            city.setName(city.getNameRu());
            city.setActive(true);
            result.put(code, cities.save(city));
        }
        return result;
    }

    private Map<String, Specialty> importSpecialties() throws IOException {
        Map<String, Specialty> result = new LinkedHashMap<>();
        for (JsonNode row : read("data/specialties.json")) {
            String code = text(row, "code");
            Specialty specialty = specialties.findByCodeIgnoreCase(code)
                    .orElseGet(() -> specialties.findByNameIgnoreCase(text(row, "nameRu")).orElseGet(Specialty::new));
            specialty.setCode(code);
            specialty.setNameRu(text(row, "nameRu"));
            specialty.setNameKk(text(row, "nameKk"));
            specialty.setNameEn(text(row, "nameEn"));
            specialty.setName(specialty.getNameRu());
            specialty.setActive(true);
            result.put(code, specialties.save(specialty));
        }
        return result;
    }

    private List<Clinic> importClinics(Map<String, City> cityByCode) throws IOException {
        Map<Long, Clinic> result = new LinkedHashMap<>();
        for (JsonNode row : readClinicRows()) {
            String externalId = firstNullableText(row, "externalSourceId", "id");
            if (!isActiveOrganization(row)) {
                if (externalId != null) clinics.findByExternalSourceId(externalId).ifPresent(clinic -> {
                    clinic.setActive(false);
                    clinics.save(clinic);
                });
                continue;
            }
            if (!row.path("isReal").asBoolean(true)) continue;
            City city = resolveCity(row, cityByCode);
            if (city == null) continue;
            String name = firstText(row, "name", "title", "orgName");
            if (name.isBlank()) continue;
            Clinic clinic = externalId == null
                    ? clinics.findByNameIgnoreCaseAndCityId(name, city.getId()).orElseGet(Clinic::new)
                    : clinics.findByExternalSourceId(externalId).orElseGet(Clinic::new);
            clinic.setName(name);
            clinic.setCity(city);
            clinic.setRegion(city.getRegion());
            clinic.setAddress(firstNullableText(row, "address", "legalAddress"));
            clinic.setType(firstNullableText(row, "type", "organizationType"));
            clinic.setSource(firstNullableText(row, "source", "dataSource"));
            if (clinic.getSource() == null) clinic.setSource(externalId == null ? "MOH_OFFICIAL" : "EGOV_OPEN_DATA");
            clinic.setExternalSourceId(externalId);
            clinic.setReal(true);
            clinic.setActive(true);
            Clinic saved = clinics.save(clinic);
            result.put(saved.getId(), saved);
        }
        return new ArrayList<>(result.values());
    }

    private List<JsonNode> readClinicRows() throws IOException {
        List<JsonNode> rows = read("data/clinics.json");
        String externalFile = System.getenv("QAMQORMED_EGOV_CLINICS_FILE");
        if (externalFile == null || externalFile.isBlank() || !Files.exists(Path.of(externalFile))) return rows;
        JsonNode root = mapper.readTree(Files.newInputStream(Path.of(externalFile)));
        if (root.isArray()) root.forEach(rows::add);
        else if (root.has("data") && root.get("data").isArray()) root.get("data").forEach(rows::add);
        return rows;
    }

    private boolean isActiveOrganization(JsonNode row) {
        if (row.has("active")) return row.get("active").asBoolean(true);
        String status = firstText(row, "status", "state", "recordStatus").toLowerCase(Locale.ROOT);
        return !(status.contains("deleted") || status.contains("removed") || status.contains("inactive")
                || status.contains("архив") || status.contains("удал"));
    }

    private City resolveCity(JsonNode row, Map<String, City> cityByCode) {
        String code = firstText(row, "cityCode", "katoCityCode");
        if (!code.isBlank() && cityByCode.containsKey(code)) return cityByCode.get(code);
        String name = firstText(row, "city", "cityName", "locality", "settlement");
        if (name.isBlank()) return null;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return cityByCode.values().stream()
                .filter(city -> List.of(city.getName(), city.getNameRu(), city.getNameKk(), city.getNameEn()).stream()
                        .filter(Objects::nonNull)
                        .map(value -> value.trim().toLowerCase(Locale.ROOT))
                        .anyMatch(normalized::equals))
                .findFirst().orElse(null);
    }

    private void linkAllDemoSpecialties(List<Clinic> catalogClinics, List<Specialty> catalogSpecialties) {
        Map<String, ClinicSpecialty> existing = clinicSpecialties.findAll().stream()
                .collect(Collectors.toMap(link -> pairKey(link.getClinic().getId(), link.getSpecialty().getId()), Function.identity(), (left, right) -> left));
        List<ClinicSpecialty> changes = new ArrayList<>();
        for (Clinic clinic : catalogClinics) {
            for (Specialty specialty : catalogSpecialties) {
                String key = pairKey(clinic.getId(), specialty.getId());
                ClinicSpecialty link = existing.get(key);
                if (link == null) {
                    link = new ClinicSpecialty();
                    link.setClinic(clinic);
                    link.setSpecialty(specialty);
                    existing.put(key, link);
                }
                link.setDemo(true);
                changes.add(link);
            }
        }
        saveClinicSpecialtiesInBatches(changes);
    }

    private void createAllDemoStaff(List<Clinic> catalogClinics, List<Specialty> catalogSpecialties) {
        Map<String, DoctorProfile> doctorBySeed = doctors.findAll().stream()
                .filter(profile -> profile.getSeedKey() != null)
                .collect(Collectors.toMap(DoctorProfile::getSeedKey, Function.identity(), (left, right) -> left));
        Map<Long, DoctorProfile> doctorByUser = doctors.findAll().stream()
                .filter(profile -> profile.getUser() != null && profile.getUser().getId() != null)
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity(), (left, right) -> left));
        Map<String, NurseProfile> nurseBySeed = nurses.findAll().stream()
                .filter(profile -> profile.getSeedKey() != null)
                .collect(Collectors.toMap(NurseProfile::getSeedKey, Function.identity(), (left, right) -> left));
        Map<Long, NurseProfile> nurseByUser = nurses.findAll().stream()
                .filter(profile -> profile.getUser() != null && profile.getUser().getId() != null)
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity(), (left, right) -> left));
        User legacyDoctor = users.findByPhone("+77000000002").orElse(null);
        User legacyNurse = users.findByPhone("+77000000003").orElse(null);
        String legacyClinicId = catalogClinics.stream().map(Clinic::getId).min(Long::compareTo).map(String::valueOf).orElse("");

        for (Clinic clinic : catalogClinics) {
            for (Specialty specialty : catalogSpecialties) {
                String doctorSeed = "DEMO_DOCTOR_" + clinic.getId() + "_" + specialty.getCode();
                String nurseSeed = "DEMO_NURSE_" + clinic.getId() + "_" + specialty.getCode();
                boolean legacyPair = String.valueOf(clinic.getId()).equals(legacyClinicId) && "CARDIOLOGY".equals(specialty.getCode());
                DoctorProfile doctor = doctorBySeed.get(doctorSeed);
                if (doctor == null && legacyPair && legacyDoctor != null) {
                    doctor = doctorByUser.get(legacyDoctor.getId());
                }
                if (doctor == null) doctor = new DoctorProfile();
                User doctorUser = doctor.getUser();
                if (doctorUser == null) doctorUser = legacyPair && legacyDoctor != null ? legacyDoctor : createSeedUser(doctorSeed, Role.DOCTOR, "+7702");
                applyDoctorSeed(doctor, doctorUser, clinic, specialty, doctorSeed);
                doctor = doctors.save(doctor);
                doctorBySeed.put(doctorSeed, doctor);
                doctorByUser.put(doctorUser.getId(), doctor);

                NurseProfile nurse = nurseBySeed.get(nurseSeed);
                if (nurse == null && legacyPair && legacyNurse != null) {
                    nurse = nurseByUser.get(legacyNurse.getId());
                }
                if (nurse == null) nurse = new NurseProfile();
                User nurseUser = nurse.getUser();
                if (nurseUser == null) nurseUser = legacyPair && legacyNurse != null ? legacyNurse : createSeedUser(nurseSeed, Role.NURSE, "+7703");
                applyNurseSeed(nurse, nurseUser, clinic, nurseSeed);
                nurse = nurses.save(nurse);
                nurseBySeed.put(nurseSeed, nurse);
                nurseByUser.put(nurseUser.getId(), nurse);
                staff.assignNurse(doctor, nurse);
            }
        }
    }

    private void applyDoctorSeed(DoctorProfile doctor, User user, Clinic clinic, Specialty specialty, String seedKey) {
        user.setRole(Role.DOCTOR);
        user.setEnabled(true);
        users.save(user);
        doctor.setUser(user);
        doctor.setClinic(clinic);
        doctor.setSpecialty(specialty);
        doctor.setExperienceYears(1 + Math.floorMod(seedKey.hashCode(), 25));
        doctor.setBio(DEMO_BIO);
        doctor.setOfficeNumber("D-" + String.format("%06d", Math.floorMod(seedKey.hashCode(), 1_000_000)));
        doctor.setSeedKey(seedKey);
        doctor.setDemo(true);
        doctor.setActive(true);
    }

    private void applyNurseSeed(NurseProfile nurse, User user, Clinic clinic, String seedKey) {
        user.setRole(Role.NURSE);
        user.setEnabled(true);
        users.save(user);
        nurse.setUser(user);
        nurse.setClinic(clinic);
        nurse.setSeedKey(seedKey);
        nurse.setDemo(true);
        nurse.setActive(true);
    }

    private User createSeedUser(String seedKey, Role role, String phonePrefix) {
        String phone = allocatePhone(seedKey, phonePrefix);
        int hash = seedKey.hashCode();
        String firstName = FIRST_NAMES[Math.floorMod(hash, FIRST_NAMES.length)];
        String lastName = LAST_NAMES[Math.floorMod(Integer.rotateLeft(hash, 7), LAST_NAMES.length)];
        String middleName = MIDDLE_NAMES[Math.floorMod(Integer.rotateLeft(hash, 13), MIDDLE_NAMES.length)];
        User user = users.findByPhone(phone).orElseGet(User::new);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setMiddleName(middleName);
        user.setPhone(phone);
        user.setPassword(demoPasswordHash);
        user.setRole(role);
        user.setEnabled(true);
        return users.save(user);
    }

    private String allocatePhone(String seedKey, String prefix) {
        int suffix = Math.floorMod(seedKey.hashCode(), 9_000_000) + 1;
        for (int attempt = 0; attempt < 9_000_000; attempt++) {
            String phone = prefix + String.format("%07d", (suffix + attempt) % 9_000_000);
            if (users.findByPhone(phone).isEmpty()) return phone;
        }
        throw new IllegalStateException("No development phone available for " + seedKey);
    }

    private void retireLegacyGeneratedStaff() {
        Set<String> preservedPhones = Set.of("+77000000002", "+77000000003");
        for (DoctorProfile doctor : doctors.findAll()) {
            if (doctor.isDemo() && doctor.getSeedKey() == null && !preservedPhones.contains(doctor.getUser().getPhone())) {
                doctor.setDemo(false);
                doctor.setActive(false);
                assignments.findByDoctorId(doctor.getId()).ifPresent(assignment -> assignment.setActive(false));
                doctors.save(doctor);
            }
        }
        for (NurseProfile nurse : nurses.findAll()) {
            if (nurse.isDemo() && nurse.getSeedKey() == null && !preservedPhones.contains(nurse.getUser().getPhone())) {
                nurse.setDemo(false);
                nurse.setActive(false);
                assignments.findByNurseId(nurse.getId()).ifPresent(assignment -> assignment.setActive(false));
                nurses.save(nurse);
            }
        }
    }

    private void retireDemoStaffOutsideCatalog(List<Clinic> catalogClinics, List<Specialty> catalogSpecialties) {
        Set<String> expectedDoctorSeeds = catalogClinics.stream()
                .flatMap(clinic -> catalogSpecialties.stream().map(specialty -> "DEMO_DOCTOR_" + clinic.getId() + "_" + specialty.getCode()))
                .collect(Collectors.toSet());
        Set<String> expectedNurseSeeds = catalogClinics.stream()
                .flatMap(clinic -> catalogSpecialties.stream().map(specialty -> "DEMO_NURSE_" + clinic.getId() + "_" + specialty.getCode()))
                .collect(Collectors.toSet());
        for (DoctorProfile doctor : doctors.findAll()) {
            if (doctor.isDemo() && doctor.getSeedKey() != null && !expectedDoctorSeeds.contains(doctor.getSeedKey())) {
                doctor.setDemo(false);
                doctor.setActive(false);
                assignments.findByDoctorId(doctor.getId()).ifPresent(assignment -> {
                    assignment.setActive(false);
                    assignments.save(assignment);
                });
                doctors.save(doctor);
            }
        }
        for (NurseProfile nurse : nurses.findAll()) {
            if (nurse.isDemo() && nurse.getSeedKey() != null && !expectedNurseSeeds.contains(nurse.getSeedKey())) {
                nurse.setDemo(false);
                nurse.setActive(false);
                assignments.findByNurseId(nurse.getId()).ifPresent(assignment -> {
                    assignment.setActive(false);
                    assignments.save(assignment);
                });
                nurses.save(nurse);
            }
        }
    }

    private void removeStaleClinicSpecialtyLinks(List<Clinic> catalogClinics, List<Specialty> catalogSpecialties) {
        Set<Long> activeClinicIds = catalogClinics.stream().map(Clinic::getId).collect(Collectors.toSet());
        Set<Long> activeSpecialtyIds = catalogSpecialties.stream().map(Specialty::getId).collect(Collectors.toSet());
        List<ClinicSpecialty> stale = clinicSpecialties.findAll().stream()
                .filter(link -> !activeClinicIds.contains(link.getClinic().getId()) || !activeSpecialtyIds.contains(link.getSpecialty().getId()))
                .toList();
        if (!stale.isEmpty()) clinicSpecialties.deleteAll(stale);
    }

    private ValidationReport validateCatalog(List<Clinic> catalogClinics, List<Specialty> catalogSpecialties) {
        Set<String> expectedPairs = catalogClinics.stream()
                .flatMap(clinic -> catalogSpecialties.stream().map(specialty -> pairKey(clinic.getId(), specialty.getId())))
                .collect(Collectors.toSet());
        List<ClinicSpecialty> links = clinicSpecialties.findAll();
        Set<String> actualPairs = links.stream().filter(link -> link.getClinic().isReal() && link.getClinic().isActive() && link.getSpecialty().isActive())
                .map(link -> pairKey(link.getClinic().getId(), link.getSpecialty().getId())).collect(Collectors.toSet());
        Set<String> doctorPairs = doctors.findAll().stream()
                .filter(profile -> profile.isDemo() && profile.isActive() && profile.getClinic().isReal() && profile.getClinic().isActive())
                .map(profile -> pairKey(profile.getClinic().getId(), profile.getSpecialty().getId())).collect(Collectors.toSet());
        Set<Long> demoDoctorIds = doctors.findAll().stream().filter(profile -> profile.isDemo() && profile.isActive()).map(DoctorProfile::getId).collect(Collectors.toSet());
        Set<Long> demoNurseIds = nurses.findAll().stream().filter(profile -> profile.isDemo() && profile.isActive()).map(NurseProfile::getId).collect(Collectors.toSet());
        List<DoctorNurseAssignment> activeAssignments = assignments.findAll().stream().filter(DoctorNurseAssignment::isActive).toList();
        long doctorsWithoutNurse = demoDoctorIds.stream().filter(id -> activeAssignments.stream().noneMatch(a -> a.getDoctor().getId().equals(id) && a.getNurse().isDemo() && a.getNurse().isActive())).count();
        long nursesWithoutDoctor = demoNurseIds.stream().filter(id -> activeAssignments.stream().noneMatch(a -> a.getNurse().getId().equals(id) && a.getDoctor().isDemo() && a.getDoctor().isActive())).count();
        long crossClinic = activeAssignments.stream().filter(a -> a.getDoctor().isDemo() && a.getNurse().isDemo() && !a.getDoctor().getClinic().getId().equals(a.getNurse().getClinic().getId())).count();
        long duplicateDoctors = duplicateCount(doctors.findAll().stream().filter(profile -> profile.isDemo() && profile.isActive()).map(profile -> pairKey(profile.getClinic().getId(), profile.getSpecialty().getId())).toList());
        long duplicateNurses = duplicateCount(nurses.findAll().stream().filter(profile -> profile.isDemo() && profile.isActive()).map(NurseProfile::getSeedKey).filter(Objects::nonNull).toList());
        long duplicateAssignments = duplicateCount(activeAssignments.stream().map(assignment -> assignment.getDoctor().getId() + ":" + assignment.getNurse().getId()).toList());
        return new ValidationReport(expectedPairs.size() - actualPairs.size(), expectedPairs.size() - doctorPairs.size(), actualPairs.size(), demoDoctorIds.size(), demoNurseIds.size(), activeAssignments.stream().filter(a -> a.getDoctor().isDemo() && a.getNurse().isDemo()).count(), doctorsWithoutNurse, nursesWithoutDoctor, crossClinic, duplicateDoctors, duplicateNurses, duplicateAssignments);
    }

    private long duplicateCount(List<String> values) { return values.size() - new HashSet<>(values).size(); }
    private String pairKey(Long clinicId, Long specialtyId) { return clinicId + ":" + specialtyId; }

    private void reportClinicImport(List<Clinic> sourceClinics, Map<String, City> cityByCode) {
        String externalFile = System.getenv("QAMQORMED_EGOV_CLINICS_FILE");
        boolean externalAvailable = externalFile != null && !externalFile.isBlank() && Files.exists(Path.of(externalFile));
        Set<String> citiesWithClinics = sourceClinics.stream().map(clinic -> clinic.getCity().getCode()).collect(Collectors.toSet());
        List<String> missingCities = cityByCode.values().stream().filter(city -> !citiesWithClinics.contains(city.getCode()))
                .sorted(Comparator.comparing(City::getName)).map(city -> city.getName() + " (" + city.getRegion().getName() + ")").toList();
        if (!externalAvailable) {
            String reason = externalFile == null || externalFile.isBlank()
                    ? "QAMQORMED_EGOV_CLINICS_FILE is not configured"
                    : "QAMQORMED_EGOV_CLINICS_FILE does not point to an existing file: " + externalFile;
            log.warn("FULL CLINIC IMPORT NOT COMPLETED: local official-name snapshot imported {} organizations; {}. Cities without a clinic in this snapshot: {}", sourceClinics.size(), reason, missingCities);
        } else {
            log.info("External clinic export processed from {}: {} organization rows resolved; cities without a resolved clinic: {}", externalFile, sourceClinics.size(), missingCities);
        }
    }

    private void deactivateLegacyDemoClinic() {
        regions.findByCodeIgnoreCase("KZ-ZHA")
                .flatMap(region -> cities.findByNameIgnoreCaseAndRegionId("Тараз", region.getId()))
                .ifPresent(city -> clinics.findByNameIgnoreCaseAndCityId("QamqorMed Demo Clinic", city.getId()).ifPresent(clinic -> {
                    clinic.setActive(false);
                    clinic.setReal(false);
                    clinic.setSource("DEMO");
                    clinics.save(clinic);
                }));
    }

    private User createUserIfMissing(String firstName, String lastName, String phone, String rawPassword, Role role) {
        return users.findByPhone(phone).map(existing -> {
            existing.setRole(role);
            existing.setEnabled(true);
            return users.save(existing);
        }).orElseGet(() -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setEnabled(true);
            return users.save(user);
        });
    }

    private void saveClinicSpecialtiesInBatches(List<ClinicSpecialty> values) {
        for (int start = 0; start < values.size(); start += 200) {
            clinicSpecialties.saveAll(values.subList(start, Math.min(start + 200, values.size())));
        }
    }

    private List<JsonNode> read(String path) throws IOException {
        List<JsonNode> values = new ArrayList<>();
        mapper.readTree(new ClassPathResource(path).getInputStream()).forEach(values::add);
        return values;
    }

    private String text(JsonNode node, String field) { return node.path(field).asText(); }
    private String firstText(JsonNode row, String... fields) { for (String field : fields) if (row.hasNonNull(field) && !row.get(field).asText().isBlank()) return row.get(field).asText(); return ""; }
    private String firstNullableText(JsonNode row, String... fields) { String value = firstText(row, fields); return value.isBlank() ? null : value; }
    private <T> T require(Map<String, T> values, String key) { T value = values.get(key); if (value == null) throw new IllegalStateException("Unknown catalog reference: " + key); return value; }

    private record ValidationReport(long missingClinicSpecialtyLinks, long missingClinicSpecialtyDoctors, long clinicSpecialtyLinks,
                                    long demoDoctors, long demoNurses, long assignments, long doctorsWithoutNurse,
                                    long nursesWithoutDoctor, long crossClinicAssignments, long duplicateDoctors,
                                    long duplicateNurses, long duplicateAssignments) {
        boolean success() {
            return missingClinicSpecialtyLinks == 0 && missingClinicSpecialtyDoctors == 0 && doctorsWithoutNurse == 0
                    && nursesWithoutDoctor == 0 && crossClinicAssignments == 0 && duplicateDoctors == 0
                    && duplicateNurses == 0 && duplicateAssignments == 0 && demoDoctors == demoNurses && demoNurses == assignments;
        }
    }
}
