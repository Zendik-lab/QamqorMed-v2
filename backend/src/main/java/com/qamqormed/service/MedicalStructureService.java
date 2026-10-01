package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class MedicalStructureService {
    private final RegionRepository regions;
    private final CityRepository cities;
    private final ClinicRepository clinics;
    private final SpecialtyRepository specialties;
    private final ClinicSpecialtyRepository clinicSpecialties;
    private final DoctorProfileRepository doctors;

    public MedicalStructureService(RegionRepository regions, CityRepository cities, ClinicRepository clinics, SpecialtyRepository specialties,
                                   ClinicSpecialtyRepository clinicSpecialties, DoctorProfileRepository doctors) {
        this.regions = regions; this.cities = cities; this.clinics = clinics; this.specialties = specialties; this.clinicSpecialties = clinicSpecialties; this.doctors = doctors;
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> publicRegions() { return regions.findAllByActiveTrueOrderByNameAsc().stream().map(RegionResponse::from).toList(); }
    @Transactional(readOnly = true)
    public RegionResponse publicRegion(Long id) { return RegionResponse.from(regionPublic(id)); }
    @Transactional(readOnly = true)
    public List<RegionResponse> adminRegions() { return regions.findAllByOrderByNameAsc().stream().map(RegionResponse::from).toList(); }
    @Transactional
    public RegionResponse createRegion(CreateRegionRequest request) {
        String code = request.code().trim().toUpperCase();
        if (regions.existsByCodeIgnoreCase(code)) throw new ConflictException("Регион с таким кодом уже существует");
        Region region = new Region(); region.setName(request.name().trim()); region.setCode(code); region.setActive(true);
        return RegionResponse.from(regions.save(region));
    }
    @Transactional
    public RegionResponse updateRegion(Long id, UpdateRegionRequest request) {
        Region region = region(id);
        String code = request.code().trim().toUpperCase();
        if (regions.existsByCodeIgnoreCaseAndIdNot(code, id)) throw new ConflictException("Регион с таким кодом уже существует");
        region.setName(request.name().trim()); region.setCode(code); region.setActive(request.active());
        return RegionResponse.from(region);
    }

    @Transactional(readOnly = true)
    public List<CityResponse> publicCities() { return cities.findAllPublic().stream().map(CityResponse::from).toList(); }
    @Transactional(readOnly = true)
    public CityResponse publicCity(Long id) { City city = city(id); requirePublic(city); return CityResponse.from(city); }
    @Transactional(readOnly = true)
    public List<CityResponse> publicCitiesByRegion(Long regionId) {
        requirePublicRegion(regionId); return cities.findPublicByRegionId(regionId).stream().map(CityResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<CityResponse> adminCities() { return cities.findAllByOrderByNameAsc().stream().map(CityResponse::from).toList(); }
    @Transactional
    public CityResponse createCity(CreateCityRequest request) {
        City city = new City(); city.setName(request.name().trim()); city.setRegion(region(request.regionId())); city.setActive(true);
        return CityResponse.from(cities.save(city));
    }
    @Transactional
    public CityResponse updateCity(Long id, UpdateCityRequest request) {
        City city = city(id); city.setName(request.name().trim()); city.setRegion(region(request.regionId())); city.setActive(request.active());
        return CityResponse.from(city);
    }

    @Transactional(readOnly = true)
    public List<ClinicResponse> publicClinics() { return clinics.findAllPublic().stream().map(ClinicResponse::from).toList(); }
    @Transactional(readOnly = true)
    public PageResponse<ClinicResponse> publicClinicsPage(int page, int size) {
        Page<Clinic> result = clinics.findPagePublic(PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("name").ascending()));
        return PageResponse.from(result, ClinicResponse::from);
    }
    @Transactional(readOnly = true)
    public ClinicResponse publicClinic(Long id) { Clinic clinic = clinic(id); requirePublic(clinic); return ClinicResponse.from(clinic); }
    @Transactional(readOnly = true)
    public List<ClinicResponse> publicClinicsByCity(Long cityId) {
        City city = city(cityId); requirePublic(city); return clinics.findPublicByCityId(cityId).stream().map(ClinicResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<ClinicResponse> publicClinicsByRegion(Long regionId) {
        requirePublicRegion(regionId); return clinics.findPublicByRegionId(regionId).stream().map(ClinicResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<ClinicResponse> publicClinicsByCityAndSpecialty(Long cityId, Long specialtyId) {
        City city = city(cityId); requirePublic(city);
        Specialty specialty = specialty(specialtyId);
        if (!specialty.isActive()) throw new NotFoundException("Специальность не найдена");
        return clinics.findPublicByCityIdAndSpecialtyId(cityId, specialtyId).stream().map(ClinicResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> publicSpecialtiesByClinic(Long clinicId) {
        Clinic clinic = clinic(clinicId); requirePublic(clinic);
        return clinicSpecialties.findPublicByClinicId(clinicId).stream().map(link -> SpecialtyResponse.from(link.getSpecialty())).toList();
    }
    @Transactional(readOnly = true)
    public List<ClinicResponse> adminClinics() { return clinics.findAll().stream().sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).map(ClinicResponse::from).toList(); }
    @Transactional(readOnly = true)
    public PageResponse<ClinicResponse> adminClinicsPage(int page, int size) {
        Page<Clinic> result = clinics.findAll(PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("name").ascending()));
        return PageResponse.from(result, ClinicResponse::from);
    }
    @Transactional
    public ClinicResponse createClinic(CreateClinicRequest request) {
        Clinic clinic = new Clinic(); clinic.setName(request.name().trim()); clinic.setCity(city(request.cityId())); clinic.setRegion(clinic.getCity().getRegion()); clinic.setReal(false); clinic.setSource("DEMO");
        applyClinic(clinic, request.address(), request.phone(), request.description()); clinic.setActive(true);
        return ClinicResponse.from(clinics.save(clinic));
    }
    @Transactional
    public ClinicResponse updateClinic(Long id, UpdateClinicRequest request) {
        Clinic clinic = clinic(id); clinic.setName(request.name().trim()); clinic.setCity(city(request.cityId())); clinic.setRegion(clinic.getCity().getRegion());
        applyClinic(clinic, request.address(), request.phone(), request.description()); clinic.setActive(request.active());
        return ClinicResponse.from(clinic);
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> publicSpecialties() { return specialties.findAllByActiveTrueOrderByNameAsc().stream().map(SpecialtyResponse::from).toList(); }
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> publicSpecialtiesByCity(Long cityId) {
        City city = city(cityId); requirePublic(city);
        return doctors.findActiveSpecialtiesByCityId(cityId).stream().map(SpecialtyResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public SpecialtyResponse publicSpecialty(Long id) { Specialty specialty = specialty(id); if (!specialty.isActive()) throw new NotFoundException("Специальность не найдена"); return SpecialtyResponse.from(specialty); }
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> adminSpecialties() { return specialties.findAllByOrderByNameAsc().stream().map(SpecialtyResponse::from).toList(); }
    @Transactional
    public SpecialtyResponse createSpecialty(CreateSpecialtyRequest request) {
        String name = request.name().trim(); if (specialties.existsByNameIgnoreCase(name)) throw new ConflictException("Такая специальность уже существует");
        Specialty specialty = new Specialty(); specialty.setName(name); specialty.setDescription(trimToNull(request.description())); specialty.setActive(true);
        return SpecialtyResponse.from(specialties.save(specialty));
    }
    @Transactional
    public SpecialtyResponse updateSpecialty(Long id, UpdateSpecialtyRequest request) {
        Specialty specialty = specialty(id); String name = request.name().trim();
        if (specialties.existsByNameIgnoreCaseAndIdNot(name, id)) throw new ConflictException("Такая специальность уже существует");
        specialty.setName(name); specialty.setDescription(trimToNull(request.description())); specialty.setActive(request.active()); return SpecialtyResponse.from(specialty);
    }

    public Region region(Long id) { return regions.findById(id).orElseThrow(() -> new NotFoundException("Регион не найден")); }
    public City city(Long id) { return cities.findById(id).orElseThrow(() -> new NotFoundException("Город не найден")); }
    public Clinic clinic(Long id) { return clinics.findById(id).orElseThrow(() -> new NotFoundException("Клиника не найдена")); }
    public Specialty specialty(Long id) { return specialties.findById(id).orElseThrow(() -> new NotFoundException("Специальность не найдена")); }
    private Region regionPublic(Long id) { Region region = region(id); if (!region.isActive()) throw new NotFoundException("Регион не найден"); return region; }
    private void requirePublicRegion(Long id) { regionPublic(id); }
    private void requirePublic(City city) { if (!city.isActive() || !city.getRegion().isActive()) throw new NotFoundException("Город не найден"); }
    private void requirePublic(Clinic clinic) { if (!clinic.isActive() || !clinic.getCity().isActive() || !clinic.getCity().getRegion().isActive()) throw new NotFoundException("Клиника не найдена"); }
    private void applyClinic(Clinic clinic, String address, String phone, String description) { clinic.setAddress(trimToNull(address)); clinic.setPhone(trimToNull(phone)); clinic.setDescription(trimToNull(description)); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
