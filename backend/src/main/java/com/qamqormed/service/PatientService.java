package com.qamqormed.service;

import com.qamqormed.dto.DependentRequest;
import com.qamqormed.dto.DependentResponse;
import com.qamqormed.dto.PatientProfileDto;
import com.qamqormed.dto.PatientSubjectResponse;
import com.qamqormed.dto.PatientProfileUpdateRequest;
import com.qamqormed.dto.PatientLocationDto;
import com.qamqormed.dto.PatientLocationUpdateRequest;
import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.City;
import com.qamqormed.entity.Gender;
import com.qamqormed.entity.PatientProfile;
import com.qamqormed.entity.Region;
import com.qamqormed.entity.User;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.DependentRepository;
import com.qamqormed.repository.MedicalRecordRepository;
import com.qamqormed.repository.UserRepository;
import com.qamqormed.repository.CityRepository;
import com.qamqormed.repository.RegionRepository;
import com.qamqormed.repository.PatientProfileRepository;
import com.qamqormed.repository.ClinicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PatientService {
    private final UserRepository users;
    private final DependentRepository dependents;
    private final MedicalRecordRepository records;
    private final RegionRepository regions;
    private final CityRepository cities;
    private final PatientProfileRepository profiles;
    private final ClinicRepository clinics;

    public PatientService(UserRepository users, DependentRepository dependents, MedicalRecordRepository records,
                          RegionRepository regions, CityRepository cities, PatientProfileRepository profiles,
                          ClinicRepository clinics) {
        this.users = users; this.dependents = dependents; this.records = records; this.regions = regions; this.cities = cities; this.profiles = profiles; this.clinics = clinics;
    }

    @Transactional(readOnly = true)
    public PatientProfileDto profile(Long userId) { return PatientProfileDto.from(profileEntity(userId)); }

    @Transactional(readOnly = true)
    public List<PatientSubjectResponse> subjects(Long userId) {
        User owner = patient(userId);
        List<PatientSubjectResponse> result = new java.util.ArrayList<>();
        result.add(PatientSubjectResponse.self(owner));
        dependents.findAllByOwnerIdAndActiveTrueOrderByLastNameAscFirstNameAsc(userId)
                .forEach(dependent -> result.add(PatientSubjectResponse.dependent(dependent)));
        return result;
    }

    @Transactional(readOnly = true)
    public PatientLocationDto location(Long userId) { return PatientLocationDto.from(patient(userId)); }

    @Transactional
    public PatientLocationDto updateLocation(Long userId, PatientLocationUpdateRequest request) {
        User user = patient(userId);
        PatientProfile profile = profileEntity(userId);
        Region region = regions.findById(request.regionId())
                .filter(Region::isActive)
                .orElseThrow(() -> new NotFoundException("Область не найдена"));
        City city = cities.findById(request.cityId())
                .filter(City::isActive)
                .orElseThrow(() -> new NotFoundException("Город не найден"));
        if (city.getRegion() == null || !city.getRegion().getId().equals(region.getId())) {
            throw new BadRequestException("Выбранный город не относится к выбранной области");
        }
        user.setPreferredRegion(region);
        user.setPreferredCity(city);
        profile.setRegion(region);
        profile.setCity(city);
        if (profile.getClinic() != null && !profile.getClinic().getCity().getId().equals(city.getId())) profile.setClinic(null);
        return PatientLocationDto.from(users.save(user));
    }

    @Transactional
    public PatientProfileDto updateProfile(Long userId, PatientProfileUpdateRequest request) {
        User user = patient(userId);
        PatientProfile profile = profileEntity(userId);
        if (request.firstName() != null) { String value = requiredText(request.firstName(), "Укажите имя"); user.setFirstName(value); profile.setFirstName(value); }
        if (request.lastName() != null) { String value = requiredText(request.lastName(), "Укажите фамилию"); user.setLastName(value); profile.setLastName(value); }
        if (request.middleName() != null) { String value = trimToNull(request.middleName()); user.setMiddleName(value); profile.setMiddleName(value); }
        if (request.dateOfBirth() != null) validateDateOfBirth(request.dateOfBirth());
        if (request.iin() != null) validateIin(request.iin());
        if (request.dateOfBirth() != null) { user.setDateOfBirth(request.dateOfBirth()); profile.setBirthDate(request.dateOfBirth()); }
        if (request.gender() != null) {
            String value = trimToNull(request.gender());
            user.setGender(value);
            profile.setGender(parseGender(value));
        }
        if (request.iin() != null) {
            String value = trimToNull(request.iin());
            if (value != null && profiles.findByIin(value).filter(existing -> !existing.getId().equals(profile.getId())).isPresent()) {
                throw new ConflictException("Пациент с таким ИИН уже существует");
            }
            user.setIin(value);
            profile.setIin(value);
        }
        if (request.email() != null) user.setEmail(trimToNull(request.email()));
        if (request.clinicId() != null) {
            var clinic = clinics.findById(request.clinicId()).filter(com.qamqormed.entity.Clinic::isActive)
                    .orElseThrow(() -> new NotFoundException("Клиника не найдена"));
            if (profile.getCity() != null && !clinic.getCity().getId().equals(profile.getCity().getId())) {
                throw new BadRequestException("Выбранная клиника не относится к городу профиля");
            }
            profile.setClinic(clinic);
        }
        if (request.address() != null) profile.setAddress(trimToNull(request.address()));
        users.save(user);
        return PatientProfileDto.from(profiles.save(profile));
    }

    @Transactional(readOnly = true)
    public List<DependentResponse> listDependents(Long userId) {
        patient(userId);
        return dependents.findAllByOwnerIdAndActiveTrueOrderByLastNameAscFirstNameAsc(userId).stream().map(DependentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Dependent dependent(Long userId, Long dependentId) {
        patient(userId);
        return dependents.findByIdAndOwnerIdAndActiveTrue(dependentId, userId)
                .orElseThrow(() -> new NotFoundException("Подопечный не найден"));
    }

    /** Compatibility helper for clients that still send patient_profiles.id. */
    @Transactional(readOnly = true)
    public boolean isOwnProfileId(Long userId, Long subjectId) {
        return subjectId != null && profileEntity(userId).getId().equals(subjectId);
    }

    @Transactional(readOnly = true)
    public boolean isOwnedDependentId(Long userId, Long subjectId) {
        return subjectId != null && dependents.findByIdAndOwnerIdAndActiveTrue(subjectId, userId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isPatientUserId(Long userId) {
        return userId != null && users.findById(userId)
                .map(user -> user.isEnabled() && user.getRole() == com.qamqormed.entity.Role.PATIENT)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public DependentResponse getDependent(Long userId, Long dependentId) { return DependentResponse.from(dependent(userId, dependentId)); }

    @Transactional
    public DependentResponse createDependent(Long userId, DependentRequest request) {
        User owner = patient(userId);
        validateDateOfBirth(request.dateOfBirth());
        String iin = normalizeIin(request.iin());
        if (iin != null && dependents.existsByOwnerIdAndIinAndActiveTrue(userId, iin)) {
            throw new ConflictException("Подопечный с таким ИИН уже существует");
        }
        Dependent dependent = new Dependent();
        apply(dependent, request, iin);
        dependent.setOwner(owner);
        dependent.setPatientProfile(profileEntity(userId));
        return DependentResponse.from(dependents.save(dependent));
    }

    @Transactional
    public DependentResponse updateDependent(Long userId, Long dependentId, DependentRequest request) {
        Dependent dependent = dependent(userId, dependentId);
        validateDateOfBirth(request.dateOfBirth());
        String iin = normalizeIin(request.iin());
        if (iin != null) {
            dependents.findByOwnerIdAndIinAndActiveTrue(userId, iin)
                    .filter(existing -> !existing.getId().equals(dependentId))
                    .ifPresent(existing -> { throw new ConflictException("Подопечный с таким ИИН уже существует"); });
        }
        apply(dependent, request, iin);
        return DependentResponse.from(dependents.save(dependent));
    }

    @Transactional
    public void deactivateDependent(Long userId, Long dependentId) {
        Dependent dependent = dependent(userId, dependentId);
        dependent.setActive(false);
        dependents.save(dependent);
    }

    @Transactional(readOnly = true)
    public List<com.qamqormed.dto.MedicalRecordResponse> ownMedicalRecords(Long userId) {
        patient(userId);
        return records.findAllByPatientUserIdAndDependentIsNullOrderByCreatedAtDesc(userId).stream()
                .map(com.qamqormed.dto.MedicalRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<com.qamqormed.dto.MedicalRecordResponse> dependentMedicalRecords(Long userId, Long dependentId) {
        dependent(userId, dependentId);
        return records.findAllByDependentIdOrderByCreatedAtDesc(dependentId).stream()
                .map(com.qamqormed.dto.MedicalRecordResponse::from).toList();
    }

    public User patient(Long userId) {
        return patientUser(userId);
    }

    @Transactional
    public PatientProfile profileEntity(Long userId) {
        User user = patientUser(userId);
        return profiles.findByUserId(userId).orElseGet(() -> {
            PatientProfile profile = new PatientProfile();
            profile.setUser(user);
            profile.setFirstName(user.getFirstName());
            profile.setLastName(user.getLastName());
            profile.setMiddleName(user.getMiddleName());
            profile.setBirthDate(user.getDateOfBirth());
            profile.setGender(parseGender(user.getGender()));
            profile.setIin(user.getIin());
            profile.setPhone(user.getPhone());
            profile.setRegion(user.getPreferredRegion());
            profile.setCity(user.getPreferredCity());
            return profiles.save(profile);
        });
    }

    private User patientUser(Long userId) {
        return users.findById(userId).filter(user -> user.isEnabled() && user.getRole() == com.qamqormed.entity.Role.PATIENT)
                .orElseThrow(() -> new NotFoundException("Пациент не найден"));
    }

    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Gender.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("Пол должен быть MALE, FEMALE или UNKNOWN"); }
    }

    public static void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) throw new BadRequestException("Укажите дату рождения");
        if (dateOfBirth.isAfter(LocalDate.now())) throw new BadRequestException("Дата рождения не может быть в будущем");
    }

    public static void validateIin(String iin) {
        if (iin != null && !iin.isBlank() && !iin.matches("\\d{12}")) {
            throw new BadRequestException("ИИН должен содержать 12 цифр");
        }
    }

    private void apply(Dependent dependent, DependentRequest request, String iin) {
        dependent.setFirstName(requiredText(request.firstName(), "Укажите имя"));
        dependent.setLastName(requiredText(request.lastName(), "Укажите фамилию"));
        dependent.setMiddleName(trimToNull(request.middleName()));
        dependent.setDateOfBirth(request.dateOfBirth());
        dependent.setGender(trimToNull(request.gender()));
        dependent.setIin(iin);
        dependent.setRelationship(request.relationship());
        dependent.setPhone(trimToNull(request.phone()));
    }

    private String normalizeIin(String value) {
        String normalized = trimToNull(value);
        validateIin(normalized);
        return normalized;
    }

    private String requiredText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new BadRequestException(message);
        return normalized;
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
