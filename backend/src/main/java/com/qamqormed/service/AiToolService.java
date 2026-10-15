package com.qamqormed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qamqormed.dto.*;
import com.qamqormed.entity.Role;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.repository.CityRepository;
import com.qamqormed.repository.ClinicRepository;
import com.qamqormed.repository.RegionRepository;
import com.qamqormed.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class AiToolService {
    private static final int MAX_DOCTORS = 20;
    private static final int MAX_SLOTS = 40;

    private final ObjectMapper objectMapper;
    private final StaffProfileService staff;
    private final TimeSlotService timeSlots;
    private final PatientService patients;
    private final AiBookingService booking;
    private final RegionRepository regions;
    private final CityRepository cities;
    private final ClinicRepository clinics;
    private final SpecialtyRepository specialties;
    private final ClinicDateService dates;

    public AiToolService(ObjectMapper objectMapper, StaffProfileService staff, TimeSlotService timeSlots,
                         PatientService patients, AiBookingService booking, RegionRepository regions,
                         CityRepository cities, ClinicRepository clinics, SpecialtyRepository specialties,
                         ClinicDateService dates) {
        this.objectMapper = objectMapper;
        this.staff = staff;
        this.timeSlots = timeSlots;
        this.patients = patients;
        this.booking = booking;
        this.regions = regions;
        this.cities = cities;
        this.clinics = clinics;
        this.specialties = specialties;
        this.dates = dates;
    }

    public AiToolExecutionResult execute(Long userId, Role role, String name, JsonNode arguments) {
        try {
            return switch (name) {
                case "search_doctors" -> success(searchDoctors(userId, read(arguments, SearchDoctorsArgs.class)));
                case "get_available_slots" -> success(availableSlots(read(arguments, AvailableSlotsArgs.class)));
                case "list_patient_subjects" -> success(patients.subjects(userId));
                case "prepare_appointment" -> requirePatient(role, () -> success(booking.prepareAppointment(userId, read(arguments, PrepareAppointmentArgs.class).request())));
                case "prepare_waitlist" -> requirePatient(role, () -> success(booking.prepareWaitlist(userId, read(arguments, PrepareWaitlistArgs.class).request(dates))));
                // Confirmation is deliberately UI-only. A model output or a sentence such as
                // “да, подтверждаю” is never sufficient to mutate medical data.
                case "confirm_appointment", "confirm_waitlist" -> failure("Подтверждение выполняется только отдельной кнопкой интерфейса.");
                default -> failure("Инструмент недоступен.");
            };
        } catch (BadRequestException | IllegalArgumentException exception) {
            return failure(exception.getMessage());
        } catch (RuntimeException exception) {
            return failure(exception.getMessage() == null ? "Операция не выполнена." : exception.getMessage());
        }
    }

    public AiAvailableSlotsResponse availableSlotsForUi(Long doctorId, LocalDate date, String timeFrom, String timeTo) {
        return availableSlots(new AvailableSlotsArgs(doctorId, date == null ? null : date.toString(), timeFrom, timeTo));
    }

    private AiDoctorSearchResponse searchDoctors(Long userId, SearchDoctorsArgs args) {
        LocalDate requestedDate = parseDate(args.date(), "дата поиска");
        if (requestedDate != null && requestedDate.isBefore(dates.today())) {
            throw new BadRequestException("Дата поиска уже прошла");
        }
        if ((args.timeFrom() != null || args.timeTo() != null) && requestedDate == null) {
            throw new BadRequestException("Для временного диапазона укажите дату");
        }
        LocalTime from = parseTime(args.timeFrom(), "начало диапазона");
        LocalTime to = parseTime(args.timeTo(), "конец диапазона");
        if (from != null && to != null && to.isBefore(from)) throw new BadRequestException("Временной диапазон указан неверно");

        Long regionId = resolveRegion(args.region());
        // Keep the method argument order explicit: region scopes the lookup,
        // userId is only used for the patient's fallback location. Passing
        // these two values in reverse made every city resolve against the
        // patient's numeric ID as if it were a region ID.
        Long cityId = resolveCity(regionId, userId, args.city());
        if (normalizeLookup(args.city()) != null && cityId != null) {
            // A city explicitly named by the user is authoritative. This also
            // repairs stale region context left by the model between turns.
            regionId = cities.findById(cityId).map(city -> city.getRegion().getId()).orElse(regionId);
        }
        Long clinicId = resolveClinic(cityId, regionId, args.clinic());
        Long specialtyId = resolveSpecialty(args.specialty());
        PageResponse<DoctorResponse> page = staff.pageDoctors(regionId, cityId, clinicId, specialtyId,
                normalize(args.doctorName()), 0, MAX_DOCTORS);

        List<AiDoctorSearchResponse.DoctorCard> result = page.content().stream()
                .map(doctor -> doctorCard(doctor, requestedDate, from, to))
                .filter(Objects::nonNull)
                .toList();
        return new AiDoctorSearchResponse(result, args.city(), args.specialty(), requestedDate);
    }

    private AiAvailableSlotsResponse availableSlots(AvailableSlotsArgs args) {
        if (args.doctorId() == null) throw new BadRequestException("Не указан врач");
        LocalDate date = parseDate(args.date(), "дата приёма");
        if (date == null) throw new BadRequestException("Укажите дату приёма");
        if (date.isBefore(dates.today())) throw new BadRequestException("Дата приёма уже прошла");
        LocalTime from = parseTime(args.timeFrom(), "начало диапазона");
        LocalTime to = parseTime(args.timeTo(), "конец диапазона");
        if (from != null && to != null && to.isBefore(from)) throw new BadRequestException("Временной диапазон указан неверно");
        DoctorResponse doctor = staff.publicDoctor(args.doctorId());
        List<TimeSlotResponse> available = timeSlots.available(args.doctorId(), date, date).stream()
                .filter(slot -> from == null || !slot.startsAt().toLocalTime().isBefore(from))
                .filter(slot -> to == null || !slot.startsAt().toLocalTime().isAfter(to))
                .limit(MAX_SLOTS)
                .toList();
        return new AiAvailableSlotsResponse(doctorCard(doctor, null, null, null), date,
                available.stream().map(slot -> new AiDoctorSearchResponse.SlotCard(slot.id(), args.doctorId(), slot.startsAt(), slot.endsAt())).toList());
    }

    private AiDoctorSearchResponse.DoctorCard doctorCard(DoctorResponse doctor, LocalDate date, LocalTime from, LocalTime to) {
        List<AiDoctorSearchResponse.SlotCard> slots = date == null ? List.of() : timeSlots.available(doctor.id(), date, date).stream()
                .filter(slot -> from == null || !slot.startsAt().toLocalTime().isBefore(from))
                .filter(slot -> to == null || !slot.startsAt().toLocalTime().isAfter(to))
                .limit(5)
                .map(slot -> new AiDoctorSearchResponse.SlotCard(slot.id(), doctor.id(), slot.startsAt(), slot.endsAt()))
                .toList();
        if (date != null && slots.isEmpty()) return null;
        return new AiDoctorSearchResponse.DoctorCard(doctor.id(), doctor.userId(),
                fullName(doctor.lastName(), doctor.firstName(), doctor.middleName()),
                new AiDoctorSearchResponse.SpecialtyCard(doctor.specialty().id(), doctor.specialty().code(), doctor.specialty().name()),
                new AiDoctorSearchResponse.ClinicCard(doctor.clinic().id(), doctor.clinic().name(), doctor.cityName(), doctor.regionName(), doctor.clinic().address()),
                slots);
    }

    private Long resolveRegion(String value) {
        String normalized = normalizeLookup(value);
        if (normalized == null) return null;
        return regions.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(region -> exactMatch(normalized, region.getName(), region.getNameRu(), region.getNameKk(), region.getNameEn(), region.getCode()))
                .findFirst()
                .or(() -> regions.findAllByActiveTrueOrderByNameAsc().stream()
                        .filter(region -> matches(normalized, region.getName(), region.getNameRu(), region.getNameKk(), region.getNameEn(), region.getCode()))
                        .findFirst())
                .map(com.qamqormed.entity.Region::getId)
                .orElseThrow(() -> new BadRequestException("Область не найдена, уточните название"));
    }

    private Long resolveCity(Long regionId, Long userId, String value) {
        String normalized = normalizeLookup(value);
        if (normalized == null) {
            try {
                var location = patients.location(userId);
                return location.cityId() != null ? location.cityId() : null;
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        List<com.qamqormed.entity.City> allCities = cities.findAllPublic();
        List<com.qamqormed.entity.City> candidates = allCities.stream()
                .filter(city -> regionId == null || city.getRegion().getId().equals(regionId))
                .toList();
        java.util.Optional<com.qamqormed.entity.City> resolved = candidates.stream()
                .filter(city -> exactMatch(normalized, city.getName(), city.getNameRu(), city.getNameKk(), city.getNameEn(), city.getCode()))
                .findFirst();
        if (resolved.isEmpty()) {
            resolved = candidates.stream()
                    .filter(city -> matches(normalized, city.getName(), city.getNameRu(), city.getNameKk(), city.getNameEn(), city.getCode()))
                    .findFirst();
        }
        // If a previous turn supplied a stale region, an explicitly named
        // city must still resolve from the complete active DB catalog.
        if (resolved.isEmpty() && regionId != null) {
            resolved = allCities.stream()
                    .filter(city -> exactMatch(normalized, city.getName(), city.getNameRu(), city.getNameKk(), city.getNameEn(), city.getCode()))
                    .findFirst();
            if (resolved.isEmpty()) {
                resolved = allCities.stream()
                        .filter(city -> matches(normalized, city.getName(), city.getNameRu(), city.getNameKk(), city.getNameEn(), city.getCode()))
                        .findFirst();
            }
        }
        return resolved
                .map(com.qamqormed.entity.City::getId)
                .orElseThrow(() -> new BadRequestException("Город не найден, уточните название"));
    }

    private Long resolveClinic(Long cityId, Long regionId, String value) {
        String normalized = normalizeLookup(value);
        if (normalized == null) return null;
        List<com.qamqormed.entity.Clinic> candidates = clinics.findAllPublic().stream()
                .filter(clinic -> cityId == null || clinic.getCity().getId().equals(cityId))
                .filter(clinic -> regionId == null || clinic.getCity().getRegion().getId().equals(regionId))
                .toList();
        return candidates.stream()
                .filter(clinic -> exactMatch(normalized, clinic.getName(), clinic.getAddress()))
                .findFirst()
                .or(() -> candidates.stream().filter(clinic -> matches(normalized, clinic.getName(), clinic.getAddress())).findFirst())
                .map(com.qamqormed.entity.Clinic::getId)
                .orElseThrow(() -> new BadRequestException("Поликлиника не найдена, уточните название"));
    }

    private Long resolveSpecialty(String value) {
        String normalized = normalizeLookup(value);
        if (normalized == null) return null;
        return specialties.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(specialty -> exactMatch(normalized, specialty.getName(), specialty.getNameRu(), specialty.getNameKk(), specialty.getNameEn(), specialty.getCode()))
                .findFirst()
                .or(() -> specialties.findAllByActiveTrueOrderByNameAsc().stream()
                        .filter(specialty -> matches(normalized, specialty.getName(), specialty.getNameRu(), specialty.getNameKk(), specialty.getNameEn(), specialty.getCode()))
                        .findFirst())
                .map(com.qamqormed.entity.Specialty::getId)
                .orElseThrow(() -> new BadRequestException("Специальность не найдена, уточните название"));
    }

    private boolean exactMatch(String query, String... values) {
        for (String value : values) {
            String normalized = normalizeLookup(value);
            if (normalized != null && normalized.equals(query)) return true;
        }
        return false;
    }

    private boolean matches(String query, String... values) {
        for (String value : values) {
            String normalized = normalizeLookup(value);
            if (normalized != null && (normalized.equals(query) || normalized.contains(query) || query.contains(normalized))) return true;
        }
        return false;
    }

    private <T> T read(JsonNode arguments, Class<T> type) {
        try { return objectMapper.treeToValue(arguments == null ? objectMapper.createObjectNode() : arguments, type); }
        catch (Exception exception) { throw new BadRequestException("Некорректные параметры инструмента"); }
    }

    private LocalDate parseDate(String value, String label) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        return dates.resolve(normalized, label);
    }

    private LocalTime parseTime(String value, String label) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        try { return LocalTime.parse(normalized.length() == 5 ? normalized + ":00" : normalized); }
        catch (RuntimeException exception) { throw new BadRequestException("Некорректное " + label); }
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT); }

    private String normalizeLookup(String value) {
        if (value == null || value.isBlank()) return null;
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\p{M}+", "")
                .replace('ё', 'е')
                .replaceAll("[\\p{P}\\p{Zs}]+", "")
                .trim();
    }

    private String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " ");
    }

    private AiToolExecutionResult requirePatient(Role role, Supplier<AiToolExecutionResult> action) {
        if (role != Role.PATIENT) return failure("Запись через AI доступна только пациенту.");
        return action.get();
    }

    private AiToolExecutionResult success(Object data) { return new AiToolExecutionResult(true, data, null); }
    private AiToolExecutionResult failure(String message) { return new AiToolExecutionResult(false, null, message == null ? "Операция не выполнена." : message); }

    public record AiToolExecutionResult(boolean success, Object data, String message) {}

    public record SearchDoctorsArgs(String region, String city, String clinic, String specialty, String doctorName,
                                    String date, String timeFrom, String timeTo) {}

    public record AvailableSlotsArgs(Long doctorId, String date, String timeFrom, String timeTo) {}

    public record PrepareAppointmentArgs(Long doctorId, Long timeSlotId, Long dependentId) {
        AiPrepareAppointmentRequest request() { return new AiPrepareAppointmentRequest(doctorId, timeSlotId, dependentId); }
    }

    public record PrepareWaitlistArgs(Long dependentId, Long clinicId, Long specialtyId, Long doctorId,
                                      String desiredDateFrom, String desiredDateTo, String preferredTimeFrom,
                                      String preferredTimeTo) {
        AiPrepareWaitlistRequest request(ClinicDateService dates) {
            return new AiPrepareWaitlistRequest(dependentId, clinicId, specialtyId, doctorId,
                    requiredDate(dates, desiredDateFrom, "начало периода"), requiredDate(dates, desiredDateTo, "конец периода"),
                    parseLocalTime(preferredTimeFrom), parseLocalTime(preferredTimeTo));
        }
        private LocalDate requiredDate(ClinicDateService dates, String value, String label) {
            LocalDate result = dates.resolve(value, label);
            if (result == null) throw new BadRequestException("Укажите " + label);
            return result;
        }
        private static LocalTime parseLocalTime(String value) {
            if (value == null || value.isBlank()) return null;
            return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
        }
    }
}
