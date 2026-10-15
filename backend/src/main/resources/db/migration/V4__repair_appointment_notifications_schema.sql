-- Repair installations where the appointment/notification feature was
-- deployed before V2 was applied, or where the schema history was incomplete.
-- This migration is additive and does not delete or rewrite patient data.

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS confirmation_status varchar(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS patient_profile_id bigint REFERENCES patient_profiles(id);
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS cancelled_at timestamptz;
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS cancelled_by_user_id bigint REFERENCES users(id);
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500);
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS confirmed_at timestamptz;
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS arrived_at timestamptz;
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS started_at timestamptz;
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS completed_at timestamptz;
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS no_show_at timestamptz;

UPDATE appointments SET confirmation_status = 'PENDING' WHERE confirmation_status IS NULL;
ALTER TABLE appointments ALTER COLUMN confirmation_status SET DEFAULT 'PENDING';
ALTER TABLE appointments ALTER COLUMN confirmation_status SET NOT NULL;
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_confirmation_status_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_confirmation_status_check
    CHECK (confirmation_status IN ('PENDING', 'CONFIRMED', 'DECLINED'));

CREATE INDEX IF NOT EXISTS idx_appointments_confirmation_status
    ON appointments(confirmation_status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_appointments_doctor_active_time_v2
    ON appointments(doctor_profile_id, appointment_at)
    WHERE status NOT IN ('CANCELLED');

CREATE TABLE IF NOT EXISTS notifications (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type varchar(50) NOT NULL,
    title varchar(200) NOT NULL,
    message varchar(2000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    read_at timestamptz,
    is_read boolean NOT NULL DEFAULT false,
    related_appointment_id bigint REFERENCES appointments(id) ON DELETE SET NULL,
    action_url varchar(500),
    event_key varchar(180) NOT NULL UNIQUE
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications(user_id, is_read);

CREATE TABLE IF NOT EXISTS appointment_status_history (
    id bigserial PRIMARY KEY,
    appointment_id bigint NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    old_status varchar(30),
    new_status varchar(30) NOT NULL,
    changed_by bigint REFERENCES users(id) ON DELETE SET NULL,
    changed_at timestamptz NOT NULL DEFAULT now(),
    reason varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_appointment_status_history_appointment
    ON appointment_status_history(appointment_id, changed_at);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    id bigserial PRIMARY KEY,
    patient_user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dependent_id bigint REFERENCES dependents(id),
    clinic_id bigint NOT NULL REFERENCES clinics(id),
    specialty_id bigint NOT NULL REFERENCES specialties(id),
    doctor_profile_id bigint REFERENCES doctor_profiles(id),
    desired_date_from date NOT NULL,
    desired_date_to date NOT NULL,
    preferred_time_from time,
    preferred_time_to time,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT waitlist_entries_status_check
        CHECK (status IN ('ACTIVE', 'NOTIFIED', 'BOOKED', 'EXPIRED', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS idx_waitlist_matching
    ON waitlist_entries(status, clinic_id, specialty_id, desired_date_from, desired_date_to);
CREATE INDEX IF NOT EXISTS idx_waitlist_patient
    ON waitlist_entries(patient_user_id);
