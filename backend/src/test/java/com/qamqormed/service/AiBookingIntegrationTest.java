package com.qamqormed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qamqormed.dto.AiAppointmentProposalResponse;
import com.qamqormed.dto.AiPrepareAppointmentRequest;
import com.qamqormed.dto.AiPrepareWaitlistRequest;
import com.qamqormed.dto.AiWaitlistProposalResponse;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.ai.enabled=true")
class AiBookingIntegrationTest {
    private static final ZoneId ZONE = ScheduleService.CLINIC_ZONE;
    private static final AtomicLong SEQUENCE = new AtomicLong(30);

    @Autowired private AiToolService tools;
    @Autowired private AiBookingService booking;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository users;
    @Autowired private RegionRepository regions;
    @Autowired private CityRepository cities;
    @Autowired private ClinicRepository clinics;
    @Autowired private SpecialtyRepository specialties;
    @Autowired private DoctorProfileRepository doctors;
    @Autowired private TimeSlotRepository slots;
    @Autowired private DependentRepository dependents;
    @Autowired private ClinicDateService dates;

    @Test
    void searchAndAvailableSlotsUseRealDatabaseRows() throws Exception {
        Fixture fixture = fixture();
        LocalDate date = nextWeekday();
        var search = tools.execute(fixture.patient().getId(), Role.PATIENT, "search_doctors", objectMapper.readTree(
                "{\"specialty\":\"" + fixture.specialty().getCode() + "\",\"date\":\"" + date + "\"}"));

        assertTrue(search.success(), search.message());
        var result = (com.qamqormed.dto.AiDoctorSearchResponse) search.data();
        assertEquals(1, result.doctors().size());
        assertEquals(fixture.doctor().getId(), result.doctors().getFirst().id());
        assertFalse(result.doctors().getFirst().nearestSlots().isEmpty());

        var available = tools.execute(fixture.patient().getId(), Role.PATIENT, "get_available_slots", objectMapper.readTree(
                "{\"doctorId\":" + fixture.doctor().getId() + ",\"date\":\"" + date + "\"}"));
        assertTrue(available.success(), available.message());
        var slots = (com.qamqormed.dto.AiAvailableSlotsResponse) available.data();
        assertTrue(slots.slots().stream().anyMatch(slot -> slot.id().equals(fixture.slot().getId())));
    }

    @Test
    void todaySearchDoesNotReturnSlotsThatAlreadyPassed() throws Exception {
        Fixture fixture = fixture();
        LocalDate today = dates.today();
        LocalDateTime now = dates.now().withSecond(0).withNano(0);
        TimeSlot past = new TimeSlot(); past.setDoctor(fixture.doctor()); past.setStartsAt(now.minusMinutes(30));
        past.setEndsAt(now.minusMinutes(1)); past.setActive(true); slots.save(past);
        TimeSlot future = new TimeSlot(); future.setDoctor(fixture.doctor()); future.setStartsAt(now.plusHours(1));
        future.setEndsAt(now.plusHours(1).plusMinutes(30)); future.setActive(true); slots.save(future);

        var result = tools.execute(fixture.patient().getId(), Role.PATIENT, "get_available_slots", objectMapper.readTree(
                "{\"doctorId\":" + fixture.doctor().getId() + ",\"date\":\"" + today + "\"}"));

        assertTrue(result.success(), result.message());
        var response = (com.qamqormed.dto.AiAvailableSlotsResponse) result.data();
        assertFalse(response.slots().stream().anyMatch(slot -> slot.id().equals(past.getId())));
        assertTrue(response.slots().stream().anyMatch(slot -> slot.id().equals(future.getId())));
    }

    @Test
    void pastCalendarDateIsRejectedByAiTool() throws Exception {
        Fixture fixture = fixture();
        var result = tools.execute(fixture.patient().getId(), Role.PATIENT, "search_doctors", objectMapper.readTree(
                "{\"date\":\"" + dates.today().minusDays(1) + "\"}"));

        assertFalse(result.success());
        assertEquals("Дата поиска уже прошла", result.message());
    }

    @Test
    void aiSearchResolvesTomorrowOnBackend() throws Exception {
        Fixture fixture = fixture();
        var result = tools.execute(fixture.patient().getId(), Role.PATIENT, "search_doctors", objectMapper.readTree(
                "{\"specialty\":\"" + fixture.specialty().getCode() + "\",\"date\":\"завтра\"}"));

        assertTrue(result.success(), result.message());
        var response = (com.qamqormed.dto.AiDoctorSearchResponse) result.data();
        assertEquals(dates.today().plusDays(1), response.requestedDate());
    }

    @Test
    void appointmentRequiresConfirmationAndRepeatedConfirmationIsIdempotent() {
        Fixture fixture = fixture();
        AiAppointmentProposalResponse proposal = booking.prepareAppointment(fixture.patient().getId(),
                new AiPrepareAppointmentRequest(fixture.doctor().getId(), fixture.slot().getId(), null));

        assertEquals("PREPARED", proposal.status());
        assertNull(proposal.appointment());
        AiAppointmentProposalResponse confirmed = booking.confirmAppointment(fixture.patient().getId(), proposal.proposalId());
        AiAppointmentProposalResponse repeated = booking.confirmAppointment(fixture.patient().getId(), proposal.proposalId());

        assertEquals("CONFIRMED", confirmed.status());
        assertNotNull(confirmed.appointment());
        assertEquals(confirmed.appointment().id(), repeated.appointment().id());
        assertThrows(ConflictException.class, () -> booking.prepareAppointment(fixture.patient().getId(),
                new AiPrepareAppointmentRequest(fixture.doctor().getId(), fixture.slot().getId(), null)));
    }

    @Test
    void cancelledOfferDoesNotCreateAppointmentAndForeignDependentIsRejected() {
        Fixture fixture = fixture();
        AiAppointmentProposalResponse proposal = booking.prepareAppointment(fixture.patient().getId(),
                new AiPrepareAppointmentRequest(fixture.doctor().getId(), fixture.slot().getId(), null));
        booking.cancelAppointment(fixture.patient().getId(), proposal.proposalId());
        assertThrows(BadRequestException.class, () -> booking.confirmAppointment(fixture.patient().getId(), proposal.proposalId()));

        User foreignOwner = user(Role.PATIENT, "+7700999" + SEQUENCE.incrementAndGet());
        Dependent foreignDependent = dependent(foreignOwner);
        assertThrows(NotFoundException.class, () -> booking.prepareAppointment(fixture.patient().getId(),
                new AiPrepareAppointmentRequest(fixture.doctor().getId(), fixture.secondSlot().getId(), foreignDependent.getId())));
    }

    @Test
    void waitlistAlsoRequiresExplicitConfirmation() {
        Fixture fixture = fixture();
        AiWaitlistProposalResponse proposal = booking.prepareWaitlist(fixture.patient().getId(), new AiPrepareWaitlistRequest(
                null, fixture.clinic().getId(), fixture.specialty().getId(), fixture.doctor().getId(),
                nextWeekday(), nextWeekday().plusDays(2), LocalTime.of(14, 0), LocalTime.of(18, 0)));
        assertEquals("PREPARED", proposal.status());
        booking.cancelWaitlist(fixture.patient().getId(), proposal.proposalId());
        assertThrows(BadRequestException.class, () -> booking.confirmWaitlist(fixture.patient().getId(), proposal.proposalId()));
    }

    private Fixture fixture() {
        long id = SEQUENCE.incrementAndGet();
        Region region = new Region(); region.setName("AI Test Region " + id); region.setCode("TEST-REGION-" + id); region.setActive(true); regions.save(region);
        City city = new City(); city.setName("AI City " + id); city.setCode("TEST-CITY-" + id); city.setRegion(region); city.setActive(true); cities.save(city);
        Clinic clinic = new Clinic(); clinic.setName("QamqorMed Test Clinic " + id); clinic.setCity(city); clinic.setRegion(region); clinic.setActive(true); clinics.save(clinic);
        Specialty specialty = new Specialty(); specialty.setName("Кардиология " + id); specialty.setCode("TEST-CARDIO-" + id); specialty.setActive(true); specialties.save(specialty);
        User doctorUser = user(Role.DOCTOR, "+7700888" + id);
        DoctorProfile doctor = new DoctorProfile(); doctor.setUser(doctorUser); doctor.setClinic(clinic); doctor.setSpecialty(specialty); doctor.setActive(true); doctor.setDemo(false); doctors.save(doctor);
        User patient = user(Role.PATIENT, "+7700777" + id);
        LocalDateTime startsAt = nextWeekday().atTime(15, 0);
        TimeSlot slot = new TimeSlot(); slot.setDoctor(doctor); slot.setStartsAt(startsAt); slot.setEndsAt(startsAt.plusMinutes(30)); slot.setActive(true); slots.save(slot);
        TimeSlot second = new TimeSlot(); second.setDoctor(doctor); second.setStartsAt(startsAt.plusMinutes(30)); second.setEndsAt(startsAt.plusMinutes(60)); second.setActive(true); slots.save(second);
        return new Fixture(patient, doctor, clinic, city, specialty, slot, second, region);
    }

    private User user(Role role, String phone) {
        User user = new User(); user.setFirstName("AI"); user.setLastName(role.name()); user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("Password123!")); user.setRole(role); user.setEnabled(true); return users.save(user);
    }

    private Dependent dependent(User owner) {
        Dependent dependent = new Dependent(); dependent.setOwner(owner); dependent.setFirstName("Child"); dependent.setLastName("Test");
        dependent.setDateOfBirth(LocalDate.now().minusYears(8)); dependent.setRelationship(RelationshipType.CHILD); dependent.setActive(true); return dependents.save(dependent);
    }

    private LocalDate nextWeekday() {
        LocalDate value = LocalDate.now(ZONE).plusDays(1);
        while (value.getDayOfWeek().getValue() > 5) value = value.plusDays(1);
        return value;
    }

    private record Fixture(User patient, DoctorProfile doctor, Clinic clinic, City city, Specialty specialty, TimeSlot slot, TimeSlot secondSlot, Region region) {}
}
