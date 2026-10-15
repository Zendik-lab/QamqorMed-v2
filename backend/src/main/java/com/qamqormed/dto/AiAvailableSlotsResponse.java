package com.qamqormed.dto;

import java.time.LocalDate;
import java.util.List;

public record AiAvailableSlotsResponse(AiDoctorSearchResponse.DoctorCard doctor, LocalDate date,
                                       List<AiDoctorSearchResponse.SlotCard> slots) {}
