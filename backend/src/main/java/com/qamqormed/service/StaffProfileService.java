package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import com.qamqormed.util.PhoneNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class StaffProfileService {
    private final UserRepository users;
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final SpecialtyRepository specialties;
    private final ClinicRepository clinics;
    private final PasswordEncoder passwordEncoder;

    public StaffProfileService(UserRepository users, DoctorProfileRepository doctors, NurseProfileRepository nurses,
                               SpecialtyRepository specialties, ClinicRepository clinics, PasswordEncoder passwordEncoder) {
        this.users = users; this.doctors = doctors; this.nurses = nurses; this.specialties = specialties; this.clinics = clinics; this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> publicDoctors(Long regionId, Long clinicId, Long specialtyId, Long cityId, String search) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return doctors.findAll((root, query, criteria) -> {
            root.fetch("user", JoinType.INNER);
            root.fetch("specialty", JoinType.INNER);
            root.fetch("clinic", JoinType.INNER);
            var user = root.join("user", JoinType.INNER);
            var specialty = root.join("specialty", JoinType.INNER);
            var clinic = root.join("clinic", JoinType.INNER);
            var city = clinic.join("city", JoinType.INNER);
            var region = city.join("region", JoinType.INNER);
            query.orderBy(criteria.asc(user.get("lastName")), criteria.asc(user.get("firstName")));
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.isTrue(root.get("active")));
            predicates.add(criteria.isTrue(user.get("enabled")));
            predicates.add(criteria.isTrue(specialty.get("active")));
            predicates.add(criteria.isTrue(clinic.get("active")));
            predicates.add(criteria.isTrue(city.get("active")));
            predicates.add(criteria.isTrue(region.get("active")));
            if (regionId != null) predicates.add(criteria.equal(region.get("id"), regionId));
            if (clinicId != null) predicates.add(criteria.equal(clinic.get("id"), clinicId));
            if (specialtyId != null) predicates.add(criteria.equal(specialty.get("id"), specialtyId));
            if (cityId != null) predicates.add(criteria.equal(city.get("id"), cityId));
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteria.or(criteria.like(criteria.lower(user.get("firstName")), pattern), criteria.like(criteria.lower(user.get("lastName")), pattern)));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        }).stream().map(DoctorResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public DoctorResponse publicDoctor(Long id) {
        DoctorProfile doctor = doctor(id);
        if (!doctor.isActive() || !doctor.getUser().isEnabled() || !doctor.getSpecialty().isActive() || !doctor.getClinic().isActive()
                || !doctor.getClinic().getCity().isActive() || !doctor.getClinic().getCity().getRegion().isActive()) {
            throw new NotFoundException("Врач не найден");
        }
        return DoctorResponse.from(doctor);
    }
    @Transactional(readOnly = true)
    public List<DoctorResponse> adminDoctors() { return doctors.findAllWithDetails().stream().map(DoctorResponse::from).toList(); }

    @Transactional
    public DoctorResponse createDoctor(CreateDoctorProfileRequest request) {
        String phone = normalizePhone(request.phone());
        ensureUniquePhone(phone);
        validatePassword(request.password());
        Specialty specialty = activeSpecialty(request.specialtyId());
        Clinic clinic = activeClinic(request.clinicId());
        User user = new User(); user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setPhone(phone); user.setPassword(passwordEncoder.encode(request.password())); user.setRole(Role.DOCTOR); user.setEnabled(true);
        users.save(user);
        DoctorProfile profile = new DoctorProfile(); profile.setUser(user); profile.setSpecialty(specialty); profile.setClinic(clinic);
        profile.setExperienceYears(request.experienceYears()); profile.setBio(trimToNull(request.bio())); profile.setOfficeNumber(trimToNull(request.officeNumber())); profile.setActive(true);
        return DoctorResponse.from(doctors.save(profile));
    }
    @Transactional
    public DoctorResponse updateDoctor(Long id, UpdateDoctorProfileRequest request) {
        DoctorProfile profile = doctor(id); profile.setSpecialty(activeSpecialty(request.specialtyId())); profile.setClinic(activeClinic(request.clinicId()));
        profile.setExperienceYears(request.experienceYears()); profile.setBio(trimToNull(request.bio())); profile.setOfficeNumber(trimToNull(request.officeNumber())); profile.setActive(request.active());
        return DoctorResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public List<NurseResponse> adminNurses() { return nurses.findAllWithDetails().stream().map(NurseResponse::from).toList(); }
    @Transactional
    public NurseResponse createNurse(CreateNurseProfileRequest request) {
        String phone = normalizePhone(request.phone()); ensureUniquePhone(phone); validatePassword(request.password());
        Clinic clinic = activeClinic(request.clinicId());
        User user = new User(); user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setPhone(phone); user.setPassword(passwordEncoder.encode(request.password())); user.setRole(Role.NURSE); user.setEnabled(true); users.save(user);
        NurseProfile profile = new NurseProfile(); profile.setUser(user); profile.setClinic(clinic); profile.setActive(true);
        return NurseResponse.from(nurses.save(profile));
    }
    @Transactional
    public NurseResponse updateNurse(Long id, UpdateNurseProfileRequest request) {
        NurseProfile profile = nurses.findById(id).orElseThrow(() -> new NotFoundException("Медсестра не найдена"));
        profile.setClinic(activeClinic(request.clinicId())); profile.setActive(request.active()); return NurseResponse.from(profile);
    }

    public DoctorProfile doctor(Long id) { return doctors.findById(id).orElseThrow(() -> new NotFoundException("Врач не найден")); }
    public DoctorProfile attachExistingDoctor(User user, Specialty specialty, Clinic clinic, int years, String bio, String office) {
        if (user.getRole() != Role.DOCTOR) throw new BadRequestException("Профиль можно создать только для пользователя с ролью DOCTOR");
        DoctorProfile profile = new DoctorProfile(); profile.setUser(user); profile.setSpecialty(specialty); profile.setClinic(clinic); profile.setExperienceYears(years); profile.setBio(bio); profile.setOfficeNumber(office); profile.setActive(true);
        return doctors.save(profile);
    }
    public NurseProfile attachExistingNurse(User user, Clinic clinic) {
        if (user.getRole() != Role.NURSE) throw new BadRequestException("Профиль можно создать только для пользователя с ролью NURSE");
        NurseProfile profile = new NurseProfile(); profile.setUser(user); profile.setClinic(clinic); profile.setActive(true); return nurses.save(profile);
    }
    private Specialty activeSpecialty(Long id) { Specialty specialty = specialties.findById(id).orElseThrow(() -> new NotFoundException("Специальность не найдена")); if (!specialty.isActive()) throw new BadRequestException("Нельзя назначить неактивную специальность"); return specialty; }
    private Clinic activeClinic(Long id) { Clinic clinic = clinics.findById(id).orElseThrow(() -> new NotFoundException("Клиника не найдена")); if (!clinic.isActive() || !clinic.getCity().isActive() || !clinic.getCity().getRegion().isActive()) throw new BadRequestException("Нельзя назначить неактивную клинику"); return clinic; }
    private void ensureUniquePhone(String phone) { if (users.existsByPhone(phone)) throw new ConflictException("Пользователь с таким номером уже существует"); }
    private String normalizePhone(String phone) { try { return PhoneNormalizer.normalize(phone); } catch (IllegalArgumentException exception) { throw new BadRequestException("Некорректный номер телефона"); } }
    private void validatePassword(String password) { if (!password.matches(".*[A-Za-zА-Яа-я].*") || !password.matches(".*\\d.*")) throw new BadRequestException("Пароль должен содержать букву и цифру"); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
