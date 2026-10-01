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
