package com.qamqormed.controller;
import com.qamqormed.dto.DoctorResponse;
import com.qamqormed.dto.NurseResponse;
import com.qamqormed.dto.PageResponse;
import com.qamqormed.service.StaffProfileService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final StaffProfileService service;
    public DoctorController(StaffProfileService service) { this.service = service; }
    @GetMapping public List<DoctorResponse> list(@RequestParam(required = false) Long regionId, @RequestParam(required = false) Long clinicId, @RequestParam(required = false) Long specialtyId,
                                                 @RequestParam(required = false) Long cityId, @RequestParam(required = false) String search,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
        return service.pageDoctors(regionId, cityId, clinicId, specialtyId, search, page, size).content();
    }
    @GetMapping("/page") public PageResponse<DoctorResponse> page(@RequestParam(required = false) Long regionId, @RequestParam(required = false) Long clinicId,
                                                                      @RequestParam(required = false) Long specialtyId, @RequestParam(required = false) Long cityId,
                                                                      @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "24") int size) {
        return service.pageDoctors(regionId, cityId, clinicId, specialtyId, search, page, size);
    }
    @GetMapping("/search") public PageResponse<DoctorResponse> search(@RequestParam Long cityId, @RequestParam Long specialtyId,
                                                                        @RequestParam(required = false) Long clinicId,
                                                                        @RequestParam(required = false) String query,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return service.pageDoctors(null, cityId, clinicId, specialtyId, query, page, Math.min(Math.max(size, 1), 50));
    }
    @GetMapping("/{id}") public DoctorResponse get(@PathVariable Long id) { return service.publicDoctor(id); }
    @GetMapping("/{id}/nurse") public NurseResponse nurse(@PathVariable Long id) { return service.publicNurseForDoctor(id); }
}
