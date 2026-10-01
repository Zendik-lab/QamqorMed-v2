package com.qamqormed.controller;

import com.qamqormed.dto.DoctorProfileUpdateRequest;
import com.qamqormed.dto.DoctorResponse;
import com.qamqormed.dto.NurseProfileUpdateRequest;
import com.qamqormed.dto.NurseResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.StaffProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class StaffProfileController {
    private final StaffProfileService staff;
    public StaffProfileController(StaffProfileService staff) { this.staff = staff; }

    @GetMapping({"/api/doctor/profile", "/api/doctors/me/profile"})
    public DoctorResponse doctor(@AuthenticationPrincipal AuthenticatedUser principal) { return staff.currentDoctor(principal.getId()); }

    @PutMapping({"/api/doctor/profile", "/api/doctors/me/profile"})
    public DoctorResponse updateDoctor(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody DoctorProfileUpdateRequest request) {
        return staff.updateCurrentDoctor(principal.getId(), request);
    }

    @GetMapping({"/api/nurse/profile", "/api/nurses/me/profile"})
    public NurseResponse nurse(@AuthenticationPrincipal AuthenticatedUser principal) { return staff.currentNurse(principal.getId()); }

    @PutMapping({"/api/nurse/profile", "/api/nurses/me/profile"})
    public NurseResponse updateNurse(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody NurseProfileUpdateRequest request) {
        return staff.updateCurrentNurse(principal.getId(), request);
    }
}
