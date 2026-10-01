package com.qamqormed.entity;

public enum AppointmentStatus {
    BOOKED,
    CHECKED_IN,
    NURSE_IN_PROGRESS,
    READY_FOR_DOCTOR,
    DOCTOR_IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW,

    /**
     * Kept temporarily so older rows can still be read while a production
     * database is being migrated. New appointments never use these values.
     */
    @Deprecated SCHEDULED,
    @Deprecated CONFIRMED,
    @Deprecated IN_PROGRESS;

    public AppointmentStatus canonical() {
        return switch (this) {
            case SCHEDULED -> BOOKED;
            case CONFIRMED -> CHECKED_IN;
            case IN_PROGRESS -> DOCTOR_IN_PROGRESS;
            default -> this;
        };
    }
}
