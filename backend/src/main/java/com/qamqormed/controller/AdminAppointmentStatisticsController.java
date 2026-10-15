package com.qamqormed.controller;
import com.qamqormed.dto.AdminAppointmentStatisticsResponse;
import com.qamqormed.service.AdminAppointmentStatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentStatisticsController {
    private final AdminAppointmentStatisticsService service;
    public AdminAppointmentStatisticsController(AdminAppointmentStatisticsService service) { this.service = service; }
    @GetMapping("/statistics")
    public AdminAppointmentStatisticsResponse stats(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(required = false) Long regionId, @RequestParam(required = false) Long cityId,
                                                    @RequestParam(required = false) Long clinicId, @RequestParam(required = false) Long doctorId) {
        return service.stats(from, to, regionId, cityId, clinicId, doctorId);
    }
}
