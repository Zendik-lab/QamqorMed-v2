-- QamqorMed clinical workflow migration.
-- Apply after family_patient_model_postgresql.sql for installations that use
-- ddl-auto=validate. It is intentionally additive and preserves all records.

UPDATE appointments SET status = 'BOOKED' WHERE status = 'SCHEDULED';
UPDATE appointments SET status = 'ARRIVED' WHERE status = 'CHECKED_IN' OR status = 'CONFIRMED';
UPDATE appointments SET status = 'IN_APPOINTMENT' WHERE status = 'DOCTOR_IN_PROGRESS' OR status = 'IN_PROGRESS';
ALTER TABLE appointments ALTER COLUMN status SET DEFAULT 'BOOKED';
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check CHECK (status IN (
    'BOOKED', 'CONFIRMED', 'ARRIVED', 'PREPARING', 'READY',
    'IN_APPOINTMENT', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
));

ALTER TABLE nurse_intakes ADD COLUMN IF NOT EXISTS blood_pressure_systolic integer;
ALTER TABLE nurse_intakes ADD COLUMN IF NOT EXISTS blood_pressure_diastolic integer;
ALTER TABLE nurse_intakes ADD COLUMN IF NOT EXISTS oxygen_saturation integer;
ALTER TABLE nurse_intakes ADD COLUMN IF NOT EXISTS complaint_summary varchar(1000);
ALTER TABLE nurse_intakes ADD COLUMN IF NOT EXISTS nurse_notes varchar(2000);
ALTER TABLE nurse_intakes ADD COLUMN IF NOT EXISTS updated_at timestamp with time zone;
UPDATE nurse_intakes SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE nurse_intakes ALTER COLUMN updated_at SET NOT NULL;

CREATE TABLE IF NOT EXISTS medical_visits (
    id bigserial PRIMARY KEY,
    appointment_id bigint NOT NULL UNIQUE REFERENCES appointments(id),
    patient_user_id bigint NOT NULL REFERENCES users(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_user_id bigint NOT NULL REFERENCES users(id),
    chief_complaint varchar(2000),
    anamnesis varchar(4000),
    diagnosis varchar(2000),
    diagnosis_code varchar(30),
    diagnosis_name varchar(2000),
    doctor_notes varchar(4000),
    recommendations varchar(4000),
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_medical_visits_patient ON medical_visits(patient_user_id);
CREATE INDEX IF NOT EXISTS idx_medical_visits_dependent ON medical_visits(dependent_id);
CREATE INDEX IF NOT EXISTS idx_medical_visits_doctor ON medical_visits(doctor_user_id);

CREATE TABLE IF NOT EXISTS treatments (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    patient_user_id bigint NOT NULL REFERENCES users(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_user_id bigint NOT NULL REFERENCES users(id),
    name varchar(500) NOT NULL,
    dosage varchar(200),
    frequency varchar(200),
    duration varchar(100),
    instructions varchar(1000),
    created_at timestamp with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS referrals (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    patient_user_id bigint NOT NULL REFERENCES users(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_user_id bigint NOT NULL REFERENCES users(id),
    referral_type varchar(30) NOT NULL,
    target_specialty_id bigint REFERENCES specialties(id),
    target_clinic_id bigint REFERENCES clinics(id),
    reason varchar(2000),
    notes varchar(2000),
    status varchar(30) NOT NULL,
    created_at timestamp with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS medical_documents (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    patient_user_id bigint NOT NULL REFERENCES users(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_user_id bigint NOT NULL REFERENCES users(id),
    type varchar(30) NOT NULL,
    title varchar(300) NOT NULL,
    description varchar(2000),
    file_path varchar(1000),
    file_url varchar(1000),
    created_at timestamp with time zone NOT NULL
);
