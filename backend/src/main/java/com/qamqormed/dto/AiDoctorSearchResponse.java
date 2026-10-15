package com.qamqormed.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiDoctorSearchResponse(List<DoctorCard> doctors, String city, String specialty, LocalDate requestedDate) {
    public record DoctorCard(Long id, Long userId, String fullName, SpecialtyCard specialty, ClinicCard clinic,
                             List<SlotCard> nearestSlots) {}
    public record SpecialtyCard(Long id, String code, String name) {}
    public record ClinicCard(Long id, String name, String cityName, String regionName, String address) {}
    public record SlotCard(Long id, Long doctorId, LocalDateTime startsAt, LocalDateTime endsAt) {}
}
