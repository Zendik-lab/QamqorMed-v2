-- Persistent doctor availability used by the patient booking flow.
CREATE TABLE IF NOT EXISTS time_slots (
    id bigserial PRIMARY KEY,
    doctor_profile_id bigint NOT NULL REFERENCES doctor_profiles(id),
    starts_at timestamp without time zone NOT NULL,
    ends_at timestamp without time zone NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uk_time_slots_doctor_start UNIQUE (doctor_profile_id, starts_at)
);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS time_slot_id bigint REFERENCES time_slots(id);
CREATE INDEX IF NOT EXISTS idx_time_slots_doctor_start ON time_slots(doctor_profile_id, starts_at);
CREATE INDEX IF NOT EXISTS idx_time_slots_active ON time_slots(is_active);
