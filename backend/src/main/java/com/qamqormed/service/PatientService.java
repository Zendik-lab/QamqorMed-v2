package com.qamqormed.service;

import com.qamqormed.dto.DependentRequest;
import com.qamqormed.dto.DependentResponse;
import com.qamqormed.dto.PatientProfileDto;
import com.qamqormed.dto.PatientProfileUpdateRequest;
import com.qamqormed.dto.PatientLocationDto;
import com.qamqormed.dto.PatientLocationUpdateRequest;
import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.City;
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

    public PatientService(UserRepository users, DependentRepository dependents, MedicalRecordRepository records,
                          RegionRepository regions, CityRepository cities) {
        this.users = users; this.dependents = dependents; this.records = records; this.regions = regions; this.cities = cities;
    }

    @Transactional(readOnly = true)
    public PatientProfileDto profile(Long userId) { return PatientProfileDto.from(patient(userId)); }

    @Transactional(readOnly = true)
    public PatientLocationDto location(Long userId) { return PatientLocationDto.from(patient(userId)); }

    @Transactional
    public PatientLocationDto updateLocation(Long userId, PatientLocationUpdateRequest request) {
        User user = patient(userId);
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
        return PatientLocationDto.from(users.save(user));
    }

    @Transactional
    public PatientProfileDto updateProfile(Long userId, PatientProfileUpdateRequest request) {
        User user = patient(userId);
        if (request.firstName() != null) user.setFirstName(requiredText(request.firstName(), "Укажите имя"));
        if (request.lastName() != null) user.setLastName(requiredText(request.lastName(), "Укажите фамилию"));
        if (request.middleName() != null) user.setMiddleName(trimToNull(request.middleName()));
        if (request.dateOfBirth() != null) validateDateOfBirth(request.dateOfBirth());
        if (request.iin() != null) validateIin(request.iin());
        user.setDateOfBirth(request.dateOfBirth() == null ? user.getDateOfBirth() : request.dateOfBirth());
        if (request.gender() != null) user.setGender(trimToNull(request.gender()));
        if (request.iin() != null) user.setIin(trimToNull(request.iin()));
        if (request.email() != null) user.setEmail(trimToNull(request.email()));
        return PatientProfileDto.from(users.save(user));
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
        return users.findById(userId).filter(User::isEnabled)
                .orElseThrow(() -> new NotFoundException("Пациент не найден"));
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
