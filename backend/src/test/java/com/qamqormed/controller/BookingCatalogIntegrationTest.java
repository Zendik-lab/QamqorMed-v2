package com.qamqormed.controller;

import com.qamqormed.entity.*;
import com.qamqormed.repository.*;
import com.qamqormed.security.JwtService;
import com.qamqormed.service.StaffProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingCatalogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RegionRepository regions;
    @Autowired CityRepository cities;
    @Autowired ClinicRepository clinics;
    @Autowired SpecialtyRepository specialties;
    @Autowired ClinicSpecialtyRepository clinicSpecialties;
    @Autowired DoctorProfileRepository doctors;
    @Autowired DoctorScheduleRepository schedules;
    @Autowired TimeSlotRepository slots;
    @Autowired StaffProfileService staff;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired ApplicationContext context;

    private Region region;
    private City city;
    private Clinic clinic;
    private Specialty specialty;
    private DoctorProfile doctor;
    private DoctorSchedule schedule;
    private LocalDate date;
    private String patientToken;

    @BeforeEach
    void fixture() {
        patientToken = jwt.generateToken(user("+77009991001", Role.PATIENT, true));
        region = new Region(); region.setName("Booking region"); region.setCode("BOOKING_TEST"); regions.save(region);
        city = new City(); city.setName("Booking city"); city.setRegion(region); cities.save(city);
        clinic = new Clinic(); clinic.setName("Booking clinic"); clinic.setCity(city); clinic.setRegion(region); clinics.save(clinic);
        specialty = new Specialty(); specialty.setName("Booking specialty"); specialties.save(specialty);
        ClinicSpecialty link = new ClinicSpecialty(); link.setClinic(clinic); link.setSpecialty(specialty); clinicSpecialties.save(link);
        doctor = new DoctorProfile(); doctor.setUser(user("+77009991002", Role.DOCTOR, false));
        doctor.setDemo(true); doctor.setClinic(clinic); doctor.setSpecialty(specialty); doctors.save(doctor);
        date = LocalDate.now(ZoneId.of("Asia/Qyzylorda")).plusDays(7).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        schedule = new DoctorSchedule(); schedule.setDoctor(doctor); schedule.setSlotDurationMinutes(30);
        interval(LocalTime.of(9, 0), LocalTime.of(12, 0), false);
        interval(LocalTime.of(10, 0), LocalTime.of(10, 30), true);
        schedules.save(schedule);
    }

    @Test
    void disabledDemoDoctorIsVisibleThroughoutCatalogAndCanBeBooked() throws Exception {
        mvc.perform(asPatient(get("/api/locations/regions"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(region.getId().intValue())));
        mvc.perform(asPatient(get("/api/regions/{id}/cities", region.getId()))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(city.getId()));
        mvc.perform(asPatient(get("/api/cities/{id}/clinics", city.getId()))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clinic.getId()));
        mvc.perform(asPatient(get("/api/clinics/{id}/specialties", clinic.getId()))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(specialty.getId()));
        assertCatalogSize(1);
        mvc.perform(asPatient(get("/api/doctors/{id}", doctor.getId()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value(true)).andExpect(jsonPath("$.password").doesNotExist());
        assertEquals(1, staff.publicDoctors(region.getId(), clinic.getId(), specialty.getId(), city.getId(), null).size());

        var available = json.readTree(mvc.perform(asPatient(slotRequest())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertFalse(available.isEmpty());
        var first = available.get(0);
        String body = """
                {"doctorId":%d,"cityId":%d,"clinicId":%d,"specialtyId":%d,"timeSlotId":%d,"appointmentAt":"%s"}
                """.formatted(doctor.getId(), city.getId(), clinic.getId(), specialty.getId(), first.path("id").asLong(), first.path("startsAt").asText());
        mvc.perform(asPatient(post("/api/patient/appointments")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.doctor.id").value(doctor.getId()));
        mvc.perform(asPatient(post("/api/patient/appointments")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(asPatient(slotRequest())).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(first.path("id").asInt()))));
        assertFalse(users.findById(doctor.getUser().getId()).orElseThrow().isEnabled());
    }

    @Test
    void disabledDemoDoctorCanBeBookedThroughLegacyTimeBasedRequest() throws Exception {
        String body = """
                {"doctorId":%d,"appointmentAt":"%sT09:00:00"}
                """.formatted(doctor.getId(), date);
        mvc.perform(asPatient(post("/api/patient/appointments")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"doctor", "specialty", "clinic", "city", "region"})
    void inactiveCatalogEntriesCannotBeBrowsedOrBooked(String entry) throws Exception {
        switch (entry) {
            case "doctor" -> doctor.setActive(false);
            case "specialty" -> specialty.setActive(false);
            case "clinic" -> clinic.setActive(false);
            case "city" -> city.setActive(false);
            case "region" -> region.setActive(false);
        }
        if (entry.equals("city") || entry.equals("region")) {
            mvc.perform(asPatient(get("/api/specialties/by-city/{id}", city.getId()))).andExpect(status().isNotFound());
            mvc.perform(asPatient(clinicSearch())).andExpect(status().isNotFound());
        } else {
            mvc.perform(asPatient(get("/api/specialties/by-city/{id}", city.getId()))).andExpect(status().isOk()).andExpect(content().json("[]"));
            mvc.perform(asPatient(clinicSearch())).andExpect(entry.equals("specialty") ? status().isNotFound() : status().isOk());
            assertTrue(clinics.findPublicByCityIdAndSpecialtyId(city.getId(), specialty.getId()).isEmpty());
        }
        mvc.perform(asPatient(doctorSearch())).andExpect(status().isOk()).andExpect(jsonPath("$.content", empty()));
        mvc.perform(asPatient(get("/api/doctors/{id}", doctor.getId()))).andExpect(status().isNotFound());
        mvc.perform(asPatient(slotRequest())).andExpect(status().isNotFound());
        mvc.perform(asPatient(post("/api/patient/appointments")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":%d,\"appointmentAt\":\"%sT09:00:00\"}".formatted(doctor.getId(), date)))
                .andExpect(status().isNotFound());
        if (entry.equals("region")) assertTrue(regions.findAllByActiveTrueOrderByNameAsc().stream().noneMatch(r -> r.getId().equals(region.getId())));
        if (entry.equals("city") || entry.equals("region")) assertTrue(cities.findPublicByRegionId(region.getId()).isEmpty());
        if (entry.equals("clinic") || entry.equals("city") || entry.equals("region")) assertTrue(clinics.findPublicByCityId(city.getId()).isEmpty());
        if (entry.equals("specialty")) assertTrue(specialties.findAllByActiveTrueOrderByNameAsc().stream().noneMatch(s -> s.getId().equals(specialty.getId())));
    }

    @Test
    void cityWithoutDoctorsReturnsEmptyLists() throws Exception {
        City emptyCity = new City(); emptyCity.setName("Empty city"); emptyCity.setRegion(region); cities.save(emptyCity);
        mvc.perform(asPatient(get("/api/specialties/by-city/{id}", emptyCity.getId()))).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(asPatient(get("/api/clinics/search").param("cityId", emptyCity.getId().toString()).param("specialtyId", specialty.getId().toString())))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(asPatient(get("/api/doctors/search").param("cityId", emptyCity.getId().toString()).param("specialtyId", specialty.getId().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", empty()));
    }

    @Test
    void breaksAndSlotsFromOldScheduleAreNotAdvertised() throws Exception {
        mvc.perform(asPatient(slotRequest())).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].startsAt", not(hasItem(date + "T10:00:00"))));
        schedule.getIntervals().clear();
        schedules.saveAndFlush(schedule);
        mvc.perform(asPatient(slotRequest())).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void missingScheduleUsesExistingDefaultGenerationForDisabledDemoDoctor() throws Exception {
        schedules.delete(schedule); schedules.flush();
        mvc.perform(asPatient(get("/api/doctors/{id}/time-slots", doctor.getId()))).andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void inactiveScheduleHasNoAvailableSlotsAndRejectsBooking() throws Exception {
        schedule.setActive(false); schedules.saveAndFlush(schedule);
        mvc.perform(asPatient(slotRequest())).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(asPatient(post("/api/patient/appointments")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":%d,\"appointmentAt\":\"%sT09:00:00\"}".formatted(doctor.getId(), date)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void developmentLoginControllerIsAbsentOutsideDevProfile() {
        assertTrue(context.getBeansOfType(DevToolsController.class).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void disabledAccountsCannotLoginOrUseAnExistingTokenForAnyRole(Role role) throws Exception {
        User account = user("+77009991003", role, true);
        String token = jwt.generateToken(account);
        account.setEnabled(false); users.saveAndFlush(account);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"BookingTest123!\"}".formatted(account.getPhone())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/" + role.name().toLowerCase() + "/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private void assertCatalogSize(int size) throws Exception {
        mvc.perform(asPatient(get("/api/specialties/by-city/{id}", city.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(size)));
        mvc.perform(asPatient(clinicSearch())).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(size)));
        mvc.perform(asPatient(doctorSearch())).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(size)));
        mvc.perform(asPatient(get("/api/doctors/page").param("cityId", city.getId().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(size)));
        mvc.perform(asPatient(get("/api/doctors").param("cityId", city.getId().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(size)));
    }

    private MockHttpServletRequestBuilder clinicSearch() {
        return get("/api/clinics/search").param("cityId", city.getId().toString()).param("specialtyId", specialty.getId().toString());
    }

    private MockHttpServletRequestBuilder doctorSearch() {
        return get("/api/doctors/search").param("cityId", city.getId().toString()).param("specialtyId", specialty.getId().toString());
    }

    private MockHttpServletRequestBuilder slotRequest() {
        return get("/api/doctors/{id}/time-slots", doctor.getId()).param("from", date.toString()).param("to", date.toString());
    }

    private MockHttpServletRequestBuilder asPatient(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + patientToken);
    }

    private void interval(LocalTime start, LocalTime end, boolean isBreak) {
        DoctorScheduleInterval value = new DoctorScheduleInterval(); value.setSchedule(schedule); value.setDayOfWeek(DayOfWeek.MONDAY);
        value.setStartTime(start); value.setEndTime(end); value.setBreakInterval(isBreak); schedule.getIntervals().add(value);
    }

    private User user(String phone, Role role, boolean enabled) {
        User value = new User(); value.setFirstName("Booking"); value.setLastName(role.name()); value.setPhone(phone);
        value.setPassword(encoder.encode("BookingTest123!")); value.setRole(role); value.setEnabled(enabled); return users.save(value);
    }
}
