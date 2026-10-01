package com.qamqormed.event;

import java.time.Instant;

/** Stable extension point for the future waitlist/reminder module. */
public record AppointmentCancelled(Long appointmentId, Long doctorId, Instant occurredAt) {}
