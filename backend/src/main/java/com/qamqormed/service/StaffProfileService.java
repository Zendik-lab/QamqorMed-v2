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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    private final DoctorNurseAssignmentRepository assignments;
    private final PasswordEncoder passwordEncoder;

    public StaffProfileService(UserRepository users, DoctorProfileRepository doctors, NurseProfileRepository nurses,
                               SpecialtyRepository specialties, ClinicRepository clinics, PasswordEncoder passwordEncoder,
                               DoctorNurseAssignmentRepository assignments) {
        this.users = users; this.doctors = doctors; this.nurses = nurses; this.specialties = specialties; this.clinics = clinics;
        this.passwordEncoder = passwordEncoder; this.assignments = assignments;
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
                predicates.add(criteria.or(criteria.like(criteria.lower(user.get("firstName")), pattern), criteria.like(criteria.lower(user.get("lastName")), pattern), criteria.like(criteria.lower(user.get("middleName")), pattern)));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        }).stream().map(this::doctorResponse).toList();
    }
    @Transactional(readOnly = true)
    public DoctorResponse publicDoctor(Long id) {
        DoctorProfile doctor = doctor(id);
        if (!doctor.isActive() || !doctor.getUser().isEnabled() || !doctor.getSpecialty().isActive() || !doctor.getClinic().isActive()
                || !doctor.getClinic().getCity().isActive() || !doctor.getClinic().getCity().getRegion().isActive()) {
            throw new NotFoundException("Врач не найден");
        }
        return doctorResponse(doctor);
    }
    @Transactional(readOnly = true)
    public List<DoctorResponse> adminDoctors() { return doctors.findAllWithDetails().stream().map(this::doctorResponse).toList(); }

    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> pageDoctors(Long regionId, Long cityId, Long clinicId, Long specialtyId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<DoctorProfile> result = doctors.findAll(doctorSpecification(regionId, cityId, clinicId, specialtyId, search, true), pageable);
        return PageResponse.from(result, this::doctorResponse);
    }

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
        profile.setExperienceYears(request.experienceYears()); profile.setBio(trimToNull(request.bio())); profile.setOfficeNumber(trimToNull(request.officeNumber())); profile.setDemo(false); profile.setActive(true);
        return DoctorResponse.from(doctors.save(profile));
    }
    @Transactional
    public DoctorResponse updateDoctor(Long id, UpdateDoctorProfileRequest request) {
        DoctorProfile profile = doctor(id); Clinic newClinic = activeClinic(request.clinicId());
        assignments.findByDoctorId(id).ifPresent(assignment -> { if (!assignment.getNurse().getClinic().getId().equals(newClinic.getId())) throw new BadRequestException("Нельзя перенести врача отдельно от назначенной медсестры"); });
        profile.setSpecialty(activeSpecialty(request.specialtyId())); profile.setClinic(newClinic);
        profile.setExperienceYears(request.experienceYears()); profile.setBio(trimToNull(request.bio())); profile.setOfficeNumber(trimToNull(request.officeNumber())); profile.setActive(request.active());
        return DoctorResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public List<NurseResponse> adminNurses() { return nurses.findAllWithDetails().stream().map(this::nurseResponse).toList(); }

    @Transactional(readOnly = true)
    public PageResponse<NurseResponse> pageNurses(Long regionId, Long cityId, Long clinicId, Long doctorId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        List<Long> nurseIds = doctorId == null ? null : assignments.findByDoctorId(doctorId).stream().map(a -> a.getNurse().getId()).toList();
        Page<NurseProfile> result = nurses.findAll((root, query, criteria) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) { root.fetch("user", JoinType.INNER); root.fetch("clinic", JoinType.INNER); }
            var user = root.join("user", JoinType.INNER); var clinic = root.join("clinic", JoinType.INNER);
            var city = clinic.join("city", JoinType.INNER); var region = city.join("region", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.isTrue(root.get("active"))); predicates.add(criteria.isTrue(user.get("enabled")));
            predicates.add(criteria.isTrue(clinic.get("active"))); predicates.add(criteria.isTrue(city.get("active"))); predicates.add(criteria.isTrue(region.get("active")));
            if (regionId != null) predicates.add(criteria.equal(region.get("id"), regionId));
            if (cityId != null) predicates.add(criteria.equal(city.get("id"), cityId));
            if (clinicId != null) predicates.add(criteria.equal(clinic.get("id"), clinicId));
            if (nurseIds != null) predicates.add(nurseIds.isEmpty() ? criteria.disjunction() : root.get("id").in(nurseIds));
            if (search != null && !search.isBlank()) { String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%"; predicates.add(criteria.or(criteria.like(criteria.lower(user.get("firstName")), pattern), criteria.like(criteria.lower(user.get("lastName")), pattern), criteria.like(criteria.lower(user.get("middleName")), pattern))); }
            return criteria.and(predicates.toArray(Predicate[]::new));
        }, pageable);
        return PageResponse.from(result, this::nurseResponse);
    }
    @Transactional
    public NurseResponse createNurse(CreateNurseProfileRequest request) {
        String phone = normalizePhone(request.phone()); ensureUniquePhone(phone); validatePassword(request.password());
        Clinic clinic = activeClinic(request.clinicId());
        User user = new User(); user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setPhone(phone); user.setPassword(passwordEncoder.encode(request.password())); user.setRole(Role.NURSE); user.setEnabled(true); users.save(user);
        NurseProfile profile = new NurseProfile(); profile.setUser(user); profile.setClinic(clinic); profile.setDemo(false); profile.setActive(true);
        return NurseResponse.from(nurses.save(profile));
    }
    @Transactional
    public NurseResponse updateNurse(Long id, UpdateNurseProfileRequest request) {
        NurseProfile profile = nurses.findById(id).orElseThrow(() -> new NotFoundException("Медсестра не найдена"));
        Clinic newClinic = activeClinic(request.clinicId()); assignments.findByNurseId(id).ifPresent(assignment -> { if (!assignment.getDoctor().getClinic().getId().equals(newClinic.getId())) throw new BadRequestException("Нельзя перенести медсестру отдельно от назначенного врача"); });
        profile.setClinic(newClinic); profile.setActive(request.active()); return nurseResponse(profile);
    }

    public DoctorProfile doctor(Long id) { return doctors.findById(id).orElseThrow(() -> new NotFoundException("Врач не найден")); }
    public NurseProfile nurse(Long id) { return nurses.findById(id).orElseThrow(() -> new NotFoundException("Медсестра не найдена")); }
    public DoctorProfile attachExistingDoctor(User user, Specialty specialty, Clinic clinic, int years, String bio, String office) {
        if (user.getRole() != Role.DOCTOR) throw new BadRequestException("Профиль можно создать только для пользователя с ролью DOCTOR");
        DoctorProfile profile = new DoctorProfile(); profile.setUser(user); profile.setSpecialty(specialty); profile.setClinic(clinic); profile.setExperienceYears(years); profile.setBio(bio); profile.setOfficeNumber(office); profile.setDemo(false); profile.setActive(true);
        return doctors.save(profile);
    }
    public NurseProfile attachExistingNurse(User user, Clinic clinic) {
        if (user.getRole() != Role.NURSE) throw new BadRequestException("Профиль можно создать только для пользователя с ролью NURSE");
        NurseProfile profile = new NurseProfile(); profile.setUser(user); profile.setClinic(clinic); profile.setDemo(false); profile.setActive(true); return nurses.save(profile);
    }

    @Transactional
    public DoctorNurseAssignment assignNurse(DoctorProfile doctor, NurseProfile nurse) {
        if (!doctor.getClinic().getId().equals(nurse.getClinic().getId())) throw new BadRequestException("Врач и медсестра должны быть в одной клинике");
        var byDoctor = assignments.findByDoctorId(doctor.getId());
        if (byDoctor.isPresent()) {
            if (!byDoctor.get().getNurse().getId().equals(nurse.getId())) throw new ConflictException("У врача уже есть назначенная медсестра");
            byDoctor.get().setActive(true);
            byDoctor.get().setUnassignedAt(null);
            return assignments.save(byDoctor.get());
        }
        var byNurse = assignments.findByNurseId(nurse.getId());
        if (byNurse.isPresent()) {
            if (!byNurse.get().getDoctor().getId().equals(doctor.getId())) throw new ConflictException("Медсестра уже закреплена за другим врачом");
            byNurse.get().setActive(true);
            byNurse.get().setUnassignedAt(null);
            return assignments.save(byNurse.get());
        }
        DoctorNurseAssignment assignment = new DoctorNurseAssignment(); assignment.setDoctor(doctor); assignment.setNurse(nurse); assignment.setActive(true); return assignments.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<com.qamqormed.dto.DoctorNursePairResponse> pairs() {
        return assignments.findAllByOrderByAssignedAtDesc().stream().map(com.qamqormed.dto.DoctorNursePairResponse::from).toList();
    }

    @Transactional
    public com.qamqormed.dto.DoctorNursePairResponse assignPair(Long doctorId, Long nurseId) {
        return com.qamqormed.dto.DoctorNursePairResponse.from(assignNurse(doctor(doctorId), nurse(nurseId)));
    }

    @Transactional
    public void removePair(Long doctorId) {
        assignments.findByDoctorId(doctorId).ifPresent(value -> { value.setActive(false); value.setUnassignedAt(java.time.Instant.now()); assignments.save(value); });
    }

    @Transactional(readOnly = true)
    public NurseResponse publicNurseForDoctor(Long doctorId) {
        DoctorProfile doctor = doctor(doctorId);
        if (!doctor.isActive() || !doctor.getClinic().isActive()) throw new NotFoundException("Врач не найден");
        var assignment = assignments.findActiveByDoctorId(doctorId).orElseThrow(() -> new NotFoundException("Назначенная медсестра не найдена"));
        return NurseResponse.from(assignment.getNurse(), doctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse currentDoctor(Long userId) {
        return doctorResponse(doctorForUser(userId));
    }

    @Transactional
    public DoctorResponse updateCurrentDoctor(Long userId, DoctorProfileUpdateRequest request) {
        DoctorProfile profile = doctorForUser(userId);
        profile.setAvatarUrl(trimToNull(request.avatarUrl()));
        profile.setAbout(trimToNull(request.about()));
        profile.setEducation(trimToNull(request.education()));
        profile.setQualificationCategory(trimToNull(request.qualificationCategory()));
        profile.setCabinetNumber(trimToNull(request.cabinetNumber()));
        if (request.consultationDurationMinutes() != null) profile.setConsultationDurationMinutes(request.consultationDurationMinutes());
        return doctorResponse(profile);
    }

    @Transactional(readOnly = true)
    public NurseResponse currentNurse(Long userId) {
        NurseProfile profile = nurses.findByUserId(userId).filter(NurseProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль медсестры не найден"));
        return nurseResponse(profile);
    }

    @Transactional
    public NurseResponse updateCurrentNurse(Long userId, NurseProfileUpdateRequest request) {
        NurseProfile profile = nurses.findByUserId(userId).filter(NurseProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль медсестры не найден"));
        profile.setAvatarUrl(trimToNull(request.avatarUrl()));
        profile.setCabinetNumber(trimToNull(request.cabinetNumber()));
        profile.setEducation(trimToNull(request.education()));
        profile.setQualification(trimToNull(request.qualification()));
        if (request.experienceYears() != null) profile.setExperienceYears(request.experienceYears());
        profile.setAbout(trimToNull(request.about()));
        return nurseResponse(profile);
    }

    private DoctorResponse doctorResponse(DoctorProfile profile) { return assignments.findActiveByDoctorId(profile.getId()).map(a -> DoctorResponse.from(profile, a.getNurse())).orElseGet(() -> DoctorResponse.from(profile)); }
    private NurseResponse nurseResponse(NurseProfile profile) { return assignments.findActiveByNurseId(profile.getId()).map(a -> NurseResponse.from(profile, a.getDoctor())).orElseGet(() -> NurseResponse.from(profile)); }

    private Specification<DoctorProfile> doctorSpecification(Long regionId, Long cityId, Long clinicId, Long specialtyId, String search, boolean publicOnly) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return (root, query, criteria) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("user", JoinType.INNER); root.fetch("specialty", JoinType.INNER); root.fetch("clinic", JoinType.INNER);
                query.orderBy(criteria.asc(root.join("user", JoinType.INNER).get("lastName")), criteria.asc(root.join("user", JoinType.INNER).get("firstName")));
            }
            var user = root.join("user", JoinType.INNER); var specialty = root.join("specialty", JoinType.INNER);
            var clinic = root.join("clinic", JoinType.INNER); var city = clinic.join("city", JoinType.INNER); var region = city.join("region", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.isTrue(root.get("active"))); predicates.add(criteria.isTrue(user.get("enabled")));
            predicates.add(criteria.isTrue(specialty.get("active"))); predicates.add(criteria.isTrue(clinic.get("active")));
            predicates.add(criteria.isTrue(city.get("active"))); predicates.add(criteria.isTrue(region.get("active")));
            if (regionId != null) predicates.add(criteria.equal(region.get("id"), regionId));
            if (clinicId != null) predicates.add(criteria.equal(clinic.get("id"), clinicId));
            if (specialtyId != null) predicates.add(criteria.equal(specialty.get("id"), specialtyId));
            if (cityId != null) predicates.add(criteria.equal(city.get("id"), cityId));
            if (normalizedSearch != null) { String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%"; predicates.add(criteria.or(criteria.like(criteria.lower(user.get("firstName")), pattern), criteria.like(criteria.lower(user.get("lastName")), pattern), criteria.like(criteria.lower(user.get("middleName")), pattern))); }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }
    private Specialty activeSpecialty(Long id) { Specialty specialty = specialties.findById(id).orElseThrow(() -> new NotFoundException("Специальность не найдена")); if (!specialty.isActive()) throw new BadRequestException("Нельзя назначить неактивную специальность"); return specialty; }
    private Clinic activeClinic(Long id) { Clinic clinic = clinics.findById(id).orElseThrow(() -> new NotFoundException("Клиника не найдена")); if (!clinic.isActive() || !clinic.getCity().isActive() || !clinic.getCity().getRegion().isActive()) throw new BadRequestException("Нельзя назначить неактивную клинику"); return clinic; }
    private void ensureUniquePhone(String phone) { if (users.existsByPhone(phone)) throw new ConflictException("Пользователь с таким номером уже существует"); }
    private String normalizePhone(String phone) { try { return PhoneNormalizer.normalize(phone); } catch (IllegalArgumentException exception) { throw new BadRequestException("Некорректный номер телефона"); } }
    private void validatePassword(String password) { if (!password.matches(".*[A-Za-zА-Яа-я].*") || !password.matches(".*\\d.*")) throw new BadRequestException("Пароль должен содержать букву и цифру"); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private DoctorProfile doctorForUser(Long userId) {
        return doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
    }
}
