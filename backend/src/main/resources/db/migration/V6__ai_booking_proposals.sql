-- Short-lived, owner-scoped AI offers. They are not appointments or waitlist entries.
CREATE TABLE IF NOT EXISTS ai_appointment_proposals (
    id bigserial PRIMARY KEY,
    owner_user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_profile_id bigint NOT NULL REFERENCES doctor_profiles(id),
    dependent_id bigint REFERENCES dependents(id),
    time_slot_id bigint NOT NULL REFERENCES time_slots(id),
    appointment_at timestamp NOT NULL,
    expires_at timestamptz NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PREPARED',
    confirmed_appointment_id bigint REFERENCES appointments(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_appointment_proposals_status_check CHECK (status IN ('PREPARED', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX IF NOT EXISTS idx_ai_appointment_proposals_owner_status
    ON ai_appointment_proposals(owner_user_id, status);
CREATE INDEX IF NOT EXISTS idx_ai_appointment_proposals_expires
    ON ai_appointment_proposals(expires_at);

CREATE TABLE IF NOT EXISTS ai_waitlist_proposals (
    id bigserial PRIMARY KEY,
    owner_user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dependent_id bigint REFERENCES dependents(id),
    clinic_id bigint NOT NULL REFERENCES clinics(id),
    specialty_id bigint NOT NULL REFERENCES specialties(id),
    doctor_profile_id bigint REFERENCES doctor_profiles(id),
    desired_date_from date NOT NULL,
    desired_date_to date NOT NULL,
    preferred_time_from time,
    preferred_time_to time,
    expires_at timestamptz NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PREPARED',
    confirmed_waitlist_id bigint REFERENCES waitlist_entries(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ai_waitlist_proposals_status_check CHECK (status IN ('PREPARED', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX IF NOT EXISTS idx_ai_waitlist_proposals_owner_status
    ON ai_waitlist_proposals(owner_user_id, status);
CREATE INDEX IF NOT EXISTS idx_ai_waitlist_proposals_expires
    ON ai_waitlist_proposals(expires_at);
