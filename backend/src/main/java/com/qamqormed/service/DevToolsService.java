package com.qamqormed.service;

import com.qamqormed.dto.AuthResponse;
import com.qamqormed.dto.CityResponse;
import com.qamqormed.dto.ClinicResponse;
import com.qamqormed.dto.DevNamedValue;
import com.qamqormed.dto.DevLoginCredentials;
import com.qamqormed.dto.DevRelationResponse;
import com.qamqormed.dto.DevSpecialtyValue;
import com.qamqormed.dto.DevUserResponse;
import com.qamqormed.dto.PageResponse;
import com.qamqormed.dto.RegionResponse;
import com.qamqormed.dto.SpecialtyResponse;
import com.qamqormed.entity.DoctorNurseAssignment;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.NurseProfile;
import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.DoctorNurseAssignmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.NurseProfileRepository;
import com.qamqormed.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Development-only data access for the role-switching panel. */
@Service
@Profile("dev")
public class DevToolsService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> LEGACY_DEMO_PASSWORDS = Map.of(
            "+77000000001", "Patient123!",
            "+77000000002", "Doctor123!",
            "+77000000003", "Nurse123!",
            "+77000000004", "Admin123!"
    );
    private static final String GENERATED_STAFF_PASSWORD = "DemoStaff123!";

    private final UserRepository users;
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final DoctorNurseAssignmentRepository assignments;
    private final MedicalStructureService structure;
    private final AuthService authService;

    public DevToolsService(UserRepository users,
                           DoctorProfileRepository doctors,
                           NurseProfileRepository nurses,
                           DoctorNurseAssignmentRepository assignments,
                           MedicalStructureService structure,
                           AuthService authService) {
        this.users = users;
        this.doctors = doctors;
        this.nurses = nurses;
        this.assignments = assignments;
        this.structure = structure;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DevUserResponse> search(String roleName,
                                                String search,
                                                Long regionId,
                                                Long cityId,
                                                Long clinicId,
                                                Long specialtyId,
                                                String issue,
                                                int page,
                                                int size) {
        Role role = parseRole(roleName);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        if (role == Role.DOCTOR) {
            Page<DoctorProfile> result = doctors.findAll(doctorSpecification(search, regionId, cityId, clinicId, specialtyId, issue), pageable);
            Map<Long, DoctorNurseAssignment> relationByDoctor = result.getContent().isEmpty()
                    ? Map.of()
                    : relationMap(
                            assignments.findActiveByDoctorIdIn(result.getContent().stream().map(DoctorProfile::getId).toList()),
                            DoctorNurseAssignment::getDoctor
                    );
            return mapPage(result, profile -> doctorResponse(profile, relationByDoctor.get(profile.getId())));
        }
        if (role == Role.NURSE) {
            Page<NurseProfile> result = nurses.findAll(nurseSpecification(search, regionId, cityId, clinicId, specialtyId, issue), pageable);
            Map<Long, DoctorNurseAssignment> relationByNurse = result.getContent().isEmpty()
                    ? Map.of()
                    : relationMap(
                            assignments.findActiveByNurseIdIn(result.getContent().stream().map(NurseProfile::getId).toList()),
                            DoctorNurseAssignment::getNurse
                    );
            return mapPage(result, profile -> nurseResponse(profile, relationByNurse.get(profile.getId())));
        }

        Page<User> result = users.findAll(userSpecification(role, search), pageable);
        return mapPage(result, this::userResponse);
    }

    @Transactional(readOnly = true)
    public DevRelationResponse relationForUser(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getRole() == Role.DOCTOR) {
            DoctorProfile profile = doctors.findByUserId(userId).orElse(null);
            return profile == null ? disconnected("Doctor profile not found") : relationForDoctor(profile);
        }
        if (user.getRole() == Role.NURSE) {
            NurseProfile profile = nurses.findByUserId(userId).orElse(null);
            return profile == null ? disconnected("Nurse profile not found") : relationForNurse(profile);
        }
        return disconnected(null);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginAsDevUser(Long userId) {
        User user = users.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new NotFoundException("DEMO-пользователь не найден"));

        if (user.getRole() == Role.DOCTOR) {
            doctors.findByUserId(userId)
                    .filter(profile -> profile.isDemo() && profile.isActive())
                    .orElseThrow(() -> new NotFoundException("DEMO-врач не найден"));
        } else if (user.getRole() == Role.NURSE) {
            nurses.findByUserId(userId)
                    .filter(profile -> profile.isDemo() && profile.isActive())
                    .orElseThrow(() -> new NotFoundException("DEMO-медсестра не найдена"));
        }

        return authService.issueTokenForValidatedUser(user);
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> regions() {
        return structure.publicRegions();
    }

    @Transactional(readOnly = true)
    public List<CityResponse> cities(Long regionId) {
        return regionId == null ? structure.publicCities() : structure.publicCitiesByRegion(regionId);
    }

    @Transactional(readOnly = true)
    public List<ClinicResponse> clinics(Long cityId, Long regionId) {
        if (cityId != null) return structure.publicClinicsByCity(cityId);
        if (regionId != null) return structure.publicClinicsByRegion(regionId);
        return structure.publicClinics();
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> specialties(Long clinicId) {
        return clinicId == null ? structure.publicSpecialties() : structure.publicSpecialtiesByClinic(clinicId);
    }

    private Specification<DoctorProfile> doctorSpecification(String search, Long regionId, Long cityId,
                                                             Long clinicId, Long specialtyId, String issue) {
        return (root, query, criteria) -> {
            fetchDoctorDetails(root, query);
            Join<DoctorProfile, User> user = root.join("user", JoinType.INNER);
            Join<?, ?> specialty = root.join("specialty", JoinType.LEFT);
            Join<?, ?> clinic = root.join("clinic", JoinType.LEFT);
            Join<?, ?> city = clinic.join("city", JoinType.LEFT);
            Join<?, ?> region = city.join("region", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.equal(user.get("role"), Role.DOCTOR));
            predicates.add(criteria.isTrue(user.get("enabled")));
            predicates.add(criteria.isTrue(root.get("demo")));
            predicates.add(criteria.isTrue(root.get("active")));
            addLocationFilters(predicates, criteria, region, city, clinic, regionId, cityId, clinicId);
            if (specialtyId != null) predicates.add(criteria.equal(specialty.get("id"), specialtyId));
            addSearch(predicates, criteria, root, user, search, root.get("id"));
            addDoctorIssueFilter(predicates, criteria, query, root, issue);
            if (!isCountQuery(query)) query.orderBy(criteria.asc(user.get("lastName")), criteria.asc(user.get("firstName")));
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<NurseProfile> nurseSpecification(String search, Long regionId, Long cityId,
                                                           Long clinicId, Long specialtyId, String issue) {
        return (root, query, criteria) -> {
            fetchNurseDetails(root, query);
            Join<NurseProfile, User> user = root.join("user", JoinType.INNER);
            Join<?, ?> clinic = root.join("clinic", JoinType.LEFT);
            Join<?, ?> city = clinic.join("city", JoinType.LEFT);
            Join<?, ?> region = city.join("region", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.equal(user.get("role"), Role.NURSE));
            predicates.add(criteria.isTrue(user.get("enabled")));
            predicates.add(criteria.isTrue(root.get("demo")));
            predicates.add(criteria.isTrue(root.get("active")));
            addLocationFilters(predicates, criteria, region, city, clinic, regionId, cityId, clinicId);
            addSearch(predicates, criteria, root, user, search, root.get("id"));
            if (specialtyId != null) addNurseSpecialtyFilter(predicates, criteria, query, root, specialtyId);
            addNurseIssueFilter(predicates, criteria, query, root, issue);
            if (!isCountQuery(query)) query.orderBy(criteria.asc(user.get("lastName")), criteria.asc(user.get("firstName")));
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<User> userSpecification(Role role, String search) {
        return (root, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.equal(root.get("role"), role));
            predicates.add(criteria.isTrue(root.get("enabled")));
            addUserSearch(predicates, criteria, root, search);
            query.orderBy(criteria.asc(root.get("lastName")), criteria.asc(root.get("firstName")));
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void fetchDoctorDetails(Root<DoctorProfile> root, CriteriaQuery<?> query) {
        if (!isCountQuery(query)) {
            root.fetch("user", JoinType.INNER);
            root.fetch("specialty", JoinType.LEFT);
            root.fetch("clinic", JoinType.LEFT);
        }
    }

    private void fetchNurseDetails(Root<NurseProfile> root, CriteriaQuery<?> query) {
        if (!isCountQuery(query)) {
            root.fetch("user", JoinType.INNER);
            root.fetch("clinic", JoinType.LEFT);
        }
    }

    private boolean isCountQuery(CriteriaQuery<?> query) {
        return query.getResultType() == Long.class || query.getResultType() == long.class;
    }

    private void addLocationFilters(List<Predicate> predicates, CriteriaBuilder criteria, Join<?, ?> region,
                                    Join<?, ?> city, Join<?, ?> clinic, Long regionId, Long cityId, Long clinicId) {
        if (regionId != null) predicates.add(criteria.equal(region.get("id"), regionId));
        if (cityId != null) predicates.add(criteria.equal(city.get("id"), cityId));
        if (clinicId != null) predicates.add(criteria.equal(clinic.get("id"), clinicId));
    }

    private void addSearch(List<Predicate> predicates, CriteriaBuilder criteria, Root<?> profile,
                           Join<?, User> user, String search, jakarta.persistence.criteria.Path<Long> profileId) {
        if (search == null || search.isBlank()) return;
        String raw = search.trim();
        List<Predicate> tokens = new ArrayList<>();
        Long numeric = parseLong(raw);
        if (numeric != null) {
            tokens.add(criteria.equal(user.get("id"), numeric));
            tokens.add(criteria.equal(profileId, numeric));
        }
        for (String token : raw.toLowerCase(Locale.ROOT).split("\\s+")) {
            String pattern = "%" + token + "%";
            tokens.add(criteria.or(
                    criteria.like(criteria.lower(user.get("firstName")), pattern),
                    criteria.like(criteria.lower(user.get("lastName")), pattern),
                    criteria.like(criteria.lower(user.get("middleName")), pattern)
            ));
        }
        predicates.add(criteria.or(tokens.toArray(Predicate[]::new)));
    }

    private void addUserSearch(List<Predicate> predicates, CriteriaBuilder criteria, Root<User> user, String search) {
        if (search == null || search.isBlank()) return;
        String raw = search.trim();
        List<Predicate> tokens = new ArrayList<>();
        Long numeric = parseLong(raw);
        if (numeric != null) tokens.add(criteria.equal(user.get("id"), numeric));
        for (String token : raw.toLowerCase(Locale.ROOT).split("\\s+")) {
            String pattern = "%" + token + "%";
            tokens.add(criteria.or(
                    criteria.like(criteria.lower(user.get("firstName")), pattern),
                    criteria.like(criteria.lower(user.get("lastName")), pattern),
                    criteria.like(criteria.lower(user.get("middleName")), pattern)
            ));
        }
        predicates.add(criteria.or(tokens.toArray(Predicate[]::new)));
    }

    private void addDoctorIssueFilter(List<Predicate> predicates, CriteriaBuilder criteria, CriteriaQuery<?> query,
                                      Root<DoctorProfile> root, String issue) {
        switch (normalizedIssue(issue)) {
            case "without-nurse" -> predicates.add(criteria.not(criteria.exists(activeAssignmentForDoctor(query, criteria, root))));
            case "without-clinic" -> predicates.add(criteria.isNull(root.get("clinic")));
            case "without-specialty" -> predicates.add(criteria.isNull(root.get("specialty")));
            default -> { }
        }
    }

    private void addNurseIssueFilter(List<Predicate> predicates, CriteriaBuilder criteria, CriteriaQuery<?> query,
                                     Root<NurseProfile> root, String issue) {
        switch (normalizedIssue(issue)) {
            case "without-doctor" -> predicates.add(criteria.not(criteria.exists(activeAssignmentForNurse(query, criteria, root))));
            case "without-clinic" -> predicates.add(criteria.isNull(root.get("clinic")));
            default -> { }
        }
    }

    private void addNurseSpecialtyFilter(List<Predicate> predicates, CriteriaBuilder criteria, CriteriaQuery<?> query,
                                         Root<NurseProfile> root, Long specialtyId) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<DoctorNurseAssignment> assignment = subquery.from(DoctorNurseAssignment.class);
        subquery.select(assignment.get("nurse").get("id"));
        subquery.where(criteria.equal(assignment.get("doctor").get("specialty").get("id"), specialtyId),
                criteria.isTrue(assignment.get("active")),
                criteria.isTrue(assignment.get("doctor").get("active")),
                criteria.isTrue(assignment.get("doctor").get("user").get("enabled")),
                criteria.isTrue(assignment.get("nurse").get("active")),
                criteria.isTrue(assignment.get("nurse").get("user").get("enabled")),
                criteria.equal(assignment.get("nurse").get("id"), root.get("id")));
        predicates.add(criteria.exists(subquery));
    }

    private Subquery<Long> activeAssignmentForDoctor(CriteriaQuery<?> query, CriteriaBuilder criteria, Root<DoctorProfile> root) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<DoctorNurseAssignment> assignment = subquery.from(DoctorNurseAssignment.class);
        subquery.select(assignment.get("id"));
        subquery.where(criteria.equal(assignment.get("doctor").get("id"), root.get("id")),
                criteria.isTrue(assignment.get("active")),
                criteria.isTrue(assignment.get("nurse").get("active")),
                criteria.isTrue(assignment.get("nurse").get("user").get("enabled")));
        return subquery;
    }

    private Subquery<Long> activeAssignmentForNurse(CriteriaQuery<?> query, CriteriaBuilder criteria, Root<NurseProfile> root) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<DoctorNurseAssignment> assignment = subquery.from(DoctorNurseAssignment.class);
        subquery.select(assignment.get("id"));
        subquery.where(criteria.equal(assignment.get("nurse").get("id"), root.get("id")),
                criteria.isTrue(assignment.get("active")),
                criteria.isTrue(assignment.get("doctor").get("active")),
                criteria.isTrue(assignment.get("doctor").get("user").get("enabled")));
        return subquery;
    }

    private DevUserResponse doctorResponse(DoctorProfile profile, DoctorNurseAssignment assignment) {
        User user = profile.getUser();
        List<String> warnings = new ArrayList<>();
        if (assignment == null) warnings.add("Nurse not assigned");
        DevRelationResponse relation = assignment == null ? disconnected("Nurse not assigned") : relationFrom(assignment);
        if (relation != null) warnings.addAll(relation.warnings());
        return new DevUserResponse(user.getId(), Role.DOCTOR.name(), fullName(user), user.getPhone(), profile.isDemo(),
                profile.isActive() && user.isEnabled(), profile.getId(), null, clinicValue(profile), cityValue(profile),
                regionValue(profile), specialtyValue(profile), credentials(user, profile.isDemo()), relation, unique(warnings));
    }

    private DevUserResponse nurseResponse(NurseProfile profile, DoctorNurseAssignment assignment) {
        User user = profile.getUser();
        List<String> warnings = new ArrayList<>();
        if (assignment == null) warnings.add("Doctor not assigned");
        DevRelationResponse relation = assignment == null ? disconnected("Doctor not assigned") : relationFrom(assignment);
        if (relation != null) warnings.addAll(relation.warnings());
        DoctorProfile doctor = assignment == null ? null : assignment.getDoctor();
        return new DevUserResponse(user.getId(), Role.NURSE.name(), fullName(user), user.getPhone(), profile.isDemo(),
                profile.isActive() && user.isEnabled(), doctor == null ? null : doctor.getId(), profile.getId(),
                clinicValue(profile), cityValue(profile), regionValue(profile),
                doctor == null ? null : specialtyValue(doctor), credentials(user, profile.isDemo()), relation, unique(warnings));
    }

    private DevUserResponse userResponse(User user) {
        boolean demo = user.getPhone() != null && user.getPhone().startsWith("+770000000");
        return new DevUserResponse(user.getId(), user.getRole().name(), fullName(user), user.getPhone(), demo,
                user.isEnabled(), null, null, null, null, null, null, credentials(user, demo), disconnected(null), List.of());
    }

    private DevLoginCredentials credentials(User user, boolean demo) {
        if (!demo || user == null || user.getPhone() == null) return null;
        String password = LEGACY_DEMO_PASSWORDS.get(user.getPhone());
        if (password == null && (user.getRole() == Role.DOCTOR || user.getRole() == Role.NURSE)) {
            password = GENERATED_STAFF_PASSWORD;
        }
        return password == null ? null : new DevLoginCredentials(user.getPhone(), password);
    }

    private DevRelationResponse relationForDoctor(DoctorProfile profile) {
        return assignments.findActiveByDoctorId(profile.getId()).map(this::relationFrom)
                .orElseGet(() -> disconnected("Nurse not assigned"));
    }

    private DevRelationResponse relationForNurse(NurseProfile profile) {
        return assignments.findActiveByNurseId(profile.getId()).map(this::relationFrom)
                .orElseGet(() -> disconnected("Doctor not assigned"));
    }

    private DevRelationResponse relationFrom(DoctorNurseAssignment assignment) {
        DoctorProfile doctor = assignment.getDoctor();
        NurseProfile nurse = assignment.getNurse();
        User doctorUser = doctor == null ? null : doctor.getUser();
        User nurseUser = nurse == null ? null : nurse.getUser();
        List<String> warnings = new ArrayList<>();
        if (doctor == null || doctorUser == null) warnings.add("Doctor missing");
        if (nurse == null || nurseUser == null) warnings.add("Nurse missing");
        if (doctor != null && nurse != null && doctor.getClinic() != null && nurse.getClinic() != null
                && !doctor.getClinic().getId().equals(nurse.getClinic().getId())) warnings.add("Clinic mismatch");
        if (!assignment.isActive()) warnings.add("Assignment inactive");
        if (doctor != null && (!doctor.isActive() || !doctorUser.isEnabled())) warnings.add("Doctor inactive");
        if (nurse != null && (!nurse.isActive() || !nurseUser.isEnabled())) warnings.add("Nurse inactive");
        var clinic = doctor == null ? null : doctor.getClinic();
        var city = clinic == null ? null : clinic.getCity();
        var region = city == null ? null : city.getRegion();
        return new DevRelationResponse(
                doctor == null ? null : doctor.getId(), nurse == null ? null : nurse.getId(),
                doctorUser == null ? null : doctorUser.getId(), nurseUser == null ? null : nurseUser.getId(),
                clinic == null ? null : clinic.getId(), doctor == null || doctor.getSpecialty() == null ? null : doctor.getSpecialty().getId(),
                assignment.getId(), doctorUser == null ? null : fullName(doctorUser), nurseUser == null ? null : fullName(nurseUser),
                doctor == null || doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName(),
                clinic == null ? null : clinic.getName(), city == null ? null : city.getName(), region == null ? null : region.getName(),
                warnings.isEmpty(), unique(warnings));
    }

    private DevRelationResponse disconnected(String warning) {
        return new DevRelationResponse(null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, warning == null ? List.of() : List.of(warning));
    }

    private DevNamedValue clinicValue(DoctorProfile profile) {
        return profile.getClinic() == null ? null : new DevNamedValue(profile.getClinic().getId(), profile.getClinic().getName());
    }

    private DevNamedValue clinicValue(NurseProfile profile) {
        return profile.getClinic() == null ? null : new DevNamedValue(profile.getClinic().getId(), profile.getClinic().getName());
    }

    private DevNamedValue cityValue(DoctorProfile profile) {
        return profile.getClinic() == null || profile.getClinic().getCity() == null ? null
                : new DevNamedValue(profile.getClinic().getCity().getId(), profile.getClinic().getCity().getName());
    }

    private DevNamedValue cityValue(NurseProfile profile) {
        return profile.getClinic() == null || profile.getClinic().getCity() == null ? null
                : new DevNamedValue(profile.getClinic().getCity().getId(), profile.getClinic().getCity().getName());
    }

    private DevNamedValue regionValue(DoctorProfile profile) {
        return profile.getClinic() == null || profile.getClinic().getCity() == null || profile.getClinic().getCity().getRegion() == null ? null
                : new DevNamedValue(profile.getClinic().getCity().getRegion().getId(), profile.getClinic().getCity().getRegion().getName());
    }

    private DevNamedValue regionValue(NurseProfile profile) {
        return profile.getClinic() == null || profile.getClinic().getCity() == null || profile.getClinic().getCity().getRegion() == null ? null
                : new DevNamedValue(profile.getClinic().getCity().getRegion().getId(), profile.getClinic().getCity().getRegion().getName());
    }

    private DevSpecialtyValue specialtyValue(DoctorProfile profile) {
        return profile.getSpecialty() == null ? null
                : new DevSpecialtyValue(profile.getSpecialty().getId(), profile.getSpecialty().getCode(), profile.getSpecialty().getName());
    }

    private <T> PageResponse<DevUserResponse> mapPage(Page<T> page, Function<T, DevUserResponse> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private <T> Map<Long, DoctorNurseAssignment> relationMap(Collection<DoctorNurseAssignment> values,
                                                               Function<DoctorNurseAssignment, T> side) {
        Map<Long, DoctorNurseAssignment> result = new HashMap<>();
        for (DoctorNurseAssignment assignment : values) {
            T profile = side.apply(assignment);
            if (profile instanceof DoctorProfile doctor) result.put(doctor.getId(), assignment);
            if (profile instanceof NurseProfile nurse) result.put(nurse.getId(), assignment);
        }
        return result;
    }

    private Role parseRole(String roleName) {
        try {
            return Role.valueOf((roleName == null ? Role.DOCTOR.name() : roleName).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Неизвестная роль");
        }
    }

    private String normalizedIssue(String issue) {
        return issue == null ? "" : issue.trim().toLowerCase(Locale.ROOT);
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String fullName(User user) {
        if (user == null) return null;
        return java.util.stream.Stream.of(user.getFirstName(), user.getMiddleName(), user.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private List<String> unique(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
    }
}
