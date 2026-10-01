-- QamqorMed Stage 7 additive migration. Run after the existing structure,
-- family_patient_model and clinical_workflow migrations.

CREATE TABLE IF NOT EXISTS patient_profiles (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    middle_name varchar(100),
    iin varchar(12) UNIQUE,
    birth_date date,
    gender varchar(20),
    phone varchar(30),
    region_id bigint REFERENCES regions(id),
    city_id bigint REFERENCES cities(id),
    clinic_id bigint REFERENCES clinics(id),
    address varchar(500),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
ALTER TABLE patient_profiles DROP CONSTRAINT IF EXISTS patient_profiles_iin_format_check;
ALTER TABLE patient_profiles ADD CONSTRAINT patient_profiles_iin_format_check CHECK (iin IS NULL OR iin ~ '^[0-9]{12}$') NOT VALID;
ALTER TABLE dependents ADD COLUMN IF NOT EXISTS patient_profile_id bigint REFERENCES patient_profiles(id);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS patient_profile_id bigint REFERENCES patient_profiles(id);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS cancelled_at timestamp with time zone;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS cancelled_by_user_id bigint REFERENCES users(id);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS cancellation_reason varchar(500);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS confirmed_at timestamp with time zone;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS arrived_at timestamp with time zone;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS started_at timestamp with time zone;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS completed_at timestamp with time zone;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS no_show_at timestamp with time zone;
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_subject_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_subject_check CHECK (num_nonnulls(patient_profile_id, dependent_id) = 1) NOT VALID;
ALTER TABLE medical_visits ADD COLUMN IF NOT EXISTS patient_profile_id bigint REFERENCES patient_profiles(id);
ALTER TABLE medical_visits ADD COLUMN IF NOT EXISTS doctor_profile_id bigint REFERENCES doctor_profiles(id);
ALTER TABLE medical_visits ADD COLUMN IF NOT EXISTS clinic_id bigint REFERENCES clinics(id);

CREATE TABLE IF NOT EXISTS icd10_codes (
    id bigserial PRIMARY KEY,
    code varchar(20) NOT NULL UNIQUE,
    name varchar(500) NOT NULL,
    description varchar(2000),
    active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS visit_diagnoses (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    icd10_code_id bigint NOT NULL REFERENCES icd10_codes(id),
    notes varchar(1000),
    primary_diagnosis boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_visit_diagnoses_visit ON visit_diagnoses(medical_visit_id);
CREATE TABLE IF NOT EXISTS prescriptions (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    title varchar(300) NOT NULL,
    description varchar(2000),
    instructions varchar(2000),
    start_date date,
    end_date date,
    created_at timestamp with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS medication_prescriptions (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    medication_name varchar(300) NOT NULL,
    dosage varchar(200),
    frequency varchar(200),
    duration varchar(100),
    instructions varchar(1000),
    start_date date,
    end_date date,
    created_at timestamp with time zone NOT NULL
);
CREATE TABLE IF NOT EXISTS lab_results (
    id bigserial PRIMARY KEY,
    patient_profile_id bigint REFERENCES patient_profiles(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_profile_id bigint NOT NULL REFERENCES doctor_profiles(id),
    medical_visit_id bigint REFERENCES medical_visits(id),
    test_name varchar(300) NOT NULL,
    test_date date,
    status varchar(30) NOT NULL,
    result varchar(4000),
    reference_range varchar(1000),
    comment varchar(2000),
    created_at timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_lab_results_patient ON lab_results(patient_profile_id);
CREATE INDEX IF NOT EXISTS idx_lab_results_dependent ON lab_results(dependent_id);
ALTER TABLE lab_results DROP CONSTRAINT IF EXISTS lab_results_subject_check;
ALTER TABLE lab_results ADD CONSTRAINT lab_results_subject_check CHECK (num_nonnulls(patient_profile_id, dependent_id) = 1) NOT VALID;
CREATE TABLE IF NOT EXISTS medical_certificates (
    id bigserial PRIMARY KEY,
    medical_visit_id bigint NOT NULL REFERENCES medical_visits(id),
    doctor_profile_id bigint NOT NULL REFERENCES doctor_profiles(id),
    patient_profile_id bigint REFERENCES patient_profiles(id),
    dependent_id bigint REFERENCES dependents(id),
    type varchar(100) NOT NULL,
    issued_at timestamp with time zone NOT NULL,
    valid_until date,
    description varchar(2000),
    document_id bigint REFERENCES medical_documents(id)
);
ALTER TABLE medical_certificates DROP CONSTRAINT IF EXISTS medical_certificates_subject_check;
ALTER TABLE medical_certificates ADD CONSTRAINT medical_certificates_subject_check CHECK (num_nonnulls(patient_profile_id, dependent_id) = 1) NOT VALID;
