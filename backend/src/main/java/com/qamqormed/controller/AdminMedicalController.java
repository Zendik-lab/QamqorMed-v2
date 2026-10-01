package com.qamqormed.controller;

import com.qamqormed.dto.*;
import com.qamqormed.service.MedicalStructureService;
import com.qamqormed.service.StaffProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminMedicalController {
    private final MedicalStructureService structure;
    private final StaffProfileService staff;
    public AdminMedicalController(MedicalStructureService structure, StaffProfileService staff) { this.structure = structure; this.staff = staff; }

    @GetMapping("/regions") public List<RegionResponse> regions() { return structure.adminRegions(); }
    @PostMapping("/regions") public ResponseEntity<RegionResponse> createRegion(@Valid @RequestBody CreateRegionRequest r) { return created(structure.createRegion(r)); }
    @PutMapping("/regions/{id}") public RegionResponse updateRegion(@PathVariable Long id, @Valid @RequestBody UpdateRegionRequest r) { return structure.updateRegion(id, r); }
    @GetMapping("/cities") public List<CityResponse> cities() { return structure.adminCities(); }
    @PostMapping("/cities") public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CreateCityRequest r) { return created(structure.createCity(r)); }
    @PutMapping("/cities/{id}") public CityResponse updateCity(@PathVariable Long id, @Valid @RequestBody UpdateCityRequest r) { return structure.updateCity(id, r); }
    @GetMapping("/clinics") public List<ClinicResponse> clinics(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return structure.adminClinicsPage(page, size).content(); }
    @GetMapping("/clinics/page") public PageResponse<ClinicResponse> clinicsPage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return structure.adminClinicsPage(page, size); }
    @PostMapping("/clinics") public ResponseEntity<ClinicResponse> createClinic(@Valid @RequestBody CreateClinicRequest r) { return created(structure.createClinic(r)); }
    @PutMapping("/clinics/{id}") public ClinicResponse updateClinic(@PathVariable Long id, @Valid @RequestBody UpdateClinicRequest r) { return structure.updateClinic(id, r); }
    @GetMapping("/specialties") public List<SpecialtyResponse> specialties() { return structure.adminSpecialties(); }
    @PostMapping("/specialties") public ResponseEntity<SpecialtyResponse> createSpecialty(@Valid @RequestBody CreateSpecialtyRequest r) { return created(structure.createSpecialty(r)); }
    @PutMapping("/specialties/{id}") public SpecialtyResponse updateSpecialty(@PathVariable Long id, @Valid @RequestBody UpdateSpecialtyRequest r) { return structure.updateSpecialty(id, r); }
    @GetMapping("/doctors") public List<DoctorResponse> doctors(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return staff.pageDoctors(null, null, null, null, null, page, size).content(); }
    @GetMapping("/doctors/page") public PageResponse<DoctorResponse> doctorsPage(@RequestParam(required = false) Long regionId, @RequestParam(required = false) Long clinicId,
                                                                                     @RequestParam(required = false) Long specialtyId, @RequestParam(required = false) Long cityId,
                                                                                     @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "50") int size) {
        return staff.pageDoctors(regionId, cityId, clinicId, specialtyId, search, page, size);
    }
    @PostMapping("/doctors") public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorProfileRequest r) { return created(staff.createDoctor(r)); }
    @PutMapping("/doctors/{id}") public DoctorResponse updateDoctor(@PathVariable Long id, @Valid @RequestBody UpdateDoctorProfileRequest r) { return staff.updateDoctor(id, r); }
    @GetMapping("/nurses") public List<NurseResponse> nurses(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return staff.pageNurses(null, null, null, null, null, page, size).content(); }
    @GetMapping("/nurses/page") public PageResponse<NurseResponse> nursesPage(@RequestParam(required = false) Long regionId, @RequestParam(required = false) Long cityId,
                                                                                 @RequestParam(required = false) Long clinicId, @RequestParam(required = false) Long doctorId,
                                                                                 @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "50") int size) {
        return staff.pageNurses(regionId, cityId, clinicId, doctorId, search, page, size);
    }
    @PostMapping("/nurses") public ResponseEntity<NurseResponse> createNurse(@Valid @RequestBody CreateNurseProfileRequest r) { return created(staff.createNurse(r)); }
    @PutMapping("/nurses/{id}") public NurseResponse updateNurse(@PathVariable Long id, @Valid @RequestBody UpdateNurseProfileRequest r) { return staff.updateNurse(id, r); }
    private <T> ResponseEntity<T> created(T body) { return ResponseEntity.status(HttpStatus.CREATED).body(body); }
}
