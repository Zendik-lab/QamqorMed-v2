package com.qamqormed.controller;

import com.qamqormed.dto.TimeSlotResponse;
import com.qamqormed.service.TimeSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class TimeSlotController {
    private final TimeSlotService timeSlots;

    public TimeSlotController(TimeSlotService timeSlots) { this.timeSlots = timeSlots; }

    @GetMapping("/{doctorId}/time-slots")
    public List<TimeSlotResponse> available(@PathVariable Long doctorId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return timeSlots.available(doctorId, from, to);
    }
}
