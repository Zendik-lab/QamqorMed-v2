-- Development-only compatibility migration. It runs before Hibernate schema update.
-- IF EXISTS keeps a brand-new local database compatible with ddl-auto=update.
ALTER TABLE IF EXISTS doctor_profiles
    ADD COLUMN IF NOT EXISTS consultation_duration_minutes integer NOT NULL DEFAULT 30;

ALTER TABLE IF EXISTS doctor_profiles
    ALTER COLUMN consultation_duration_minutes SET DEFAULT 30;

ALTER TABLE IF EXISTS doctor_profiles
    ALTER COLUMN consultation_duration_minutes SET NOT NULL;

ALTER TABLE IF EXISTS nurse_profiles
    ADD COLUMN IF NOT EXISTS experience_years integer NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS nurse_profiles
    ALTER COLUMN experience_years SET DEFAULT 0;

ALTER TABLE IF EXISTS nurse_profiles
    ALTER COLUMN experience_years SET NOT NULL;

-- Keep the development database compatible with the non-null ICD-10 entity
-- field when the table already contains legacy rows.
ALTER TABLE IF EXISTS icd10_codes
    ADD COLUMN IF NOT EXISTS selectable boolean DEFAULT true;

-- Booking/notification compatibility for an existing development database.
-- Production uses the versioned Flyway migrations; this block only keeps the
-- default dev profile from running an entity INSERT against an old schema.
ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS confirmation_status varchar(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS patient_profile_id bigint REFERENCES patient_profiles(id);

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS cancelled_at timestamp with time zone;

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS cancelled_by_user_id bigint REFERENCES users(id);

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500);

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS confirmed_at timestamp with time zone;

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS arrived_at timestamp with time zone;

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS started_at timestamp with time zone;

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS completed_at timestamp with time zone;

ALTER TABLE IF EXISTS appointments
    ADD COLUMN IF NOT EXISTS no_show_at timestamp with time zone;
