package com.qamqormed.controller;
import com.qamqormed.dto.DoctorResponse;
import com.qamqormed.service.StaffProfileService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final StaffProfileService service;
    public DoctorController(StaffProfileService service) { this.service = service; }
    @GetMapping public List<DoctorResponse> list(@RequestParam(required = false) Long regionId, @RequestParam(required = false) Long clinicId, @RequestParam(required = false) Long specialtyId,
                                                 @RequestParam(required = false) Long cityId, @RequestParam(required = false) String search) {
        return service.publicDoctors(regionId, clinicId, specialtyId, cityId, search);
    }
    @GetMapping("/{id}") public DoctorResponse get(@PathVariable Long id) { return service.publicDoctor(id); }
}
