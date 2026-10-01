package com.qamqormed.entity;

public enum AppointmentStatus {
    BOOKED,
    CONFIRMED,
    ARRIVED,
    PREPARING,
    READY,
    IN_APPOINTMENT,
    COMPLETED,
    CANCELLED,
    NO_SHOW,

    /**
     * Kept temporarily so older rows can still be read while a production
     * database is being migrated. New appointments never use these values.
     */
    @Deprecated SCHEDULED,
    @Deprecated CHECKED_IN,
    @Deprecated NURSE_IN_PROGRESS,
    @Deprecated READY_FOR_DOCTOR,
    @Deprecated DOCTOR_IN_PROGRESS,
    @Deprecated IN_PROGRESS;

    public AppointmentStatus canonical() {
        return switch (this) {
            case SCHEDULED -> BOOKED;
            case CHECKED_IN -> ARRIVED;
            case NURSE_IN_PROGRESS -> PREPARING;
            case READY_FOR_DOCTOR -> READY;
            case DOCTOR_IN_PROGRESS, IN_PROGRESS -> IN_APPOINTMENT;
            default -> this;
        };
    }
}
