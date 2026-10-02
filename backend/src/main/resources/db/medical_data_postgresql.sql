-- QamqorMed medical data extension. Apply after stage7_postgresql.sql and
-- clinical_workflow_postgresql.sql. Additive only; existing medical records
-- remain readable and are never physically deleted.
--
-- This file is also the repair migration for an existing PostgreSQL database
-- that fails while starting a visit with SQLState 42703 and
-- "column r1_0.updated_at does not exist". The Referral entity and the
-- referrals table must be migrated together before the backend is restarted.

ALTER TABLE IF EXISTS visit_diagnoses ADD COLUMN IF NOT EXISTS diagnosis_type varchar(30) NOT NULL DEFAULT 'PRIMARY';
ALTER TABLE IF EXISTS visit_diagnoses ADD COLUMN IF NOT EXISTS status varchar(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE IF EXISTS visit_diagnoses ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE IF EXISTS prescriptions ADD COLUMN IF NOT EXISTS status varchar(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE IF EXISTS prescriptions ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE IF EXISTS medication_prescriptions ADD COLUMN IF NOT EXISTS dosage_unit varchar(50);
ALTER TABLE IF EXISTS medication_prescriptions ADD COLUMN IF NOT EXISTS route varchar(100);
ALTER TABLE IF EXISTS medication_prescriptions ADD COLUMN IF NOT EXISTS duration_days integer;
ALTER TABLE IF EXISTS medication_prescriptions ADD COLUMN IF NOT EXISTS status varchar(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE IF EXISTS medication_prescriptions ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE IF EXISTS lab_results ADD COLUMN IF NOT EXISTS test_code varchar(80);
ALTER TABLE IF EXISTS lab_results ADD COLUMN IF NOT EXISTS ordered_at timestamptz;
ALTER TABLE IF EXISTS lab_results ADD COLUMN IF NOT EXISTS completed_at timestamptz;
ALTER TABLE IF EXISTS lab_results ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
UPDATE lab_results SET ordered_at = COALESCE(ordered_at, created_at) WHERE ordered_at IS NULL;

ALTER TABLE IF EXISTS referrals ADD COLUMN IF NOT EXISTS target_doctor_id bigint REFERENCES doctor_profiles(id);
ALTER TABLE IF EXISTS referrals ADD COLUMN IF NOT EXISTS issued_at timestamptz;
ALTER TABLE IF EXISTS referrals ADD COLUMN IF NOT EXISTS valid_until date;
ALTER TABLE IF EXISTS referrals ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
UPDATE referrals SET issued_at = COALESCE(issued_at, created_at) WHERE issued_at IS NULL;

ALTER TABLE IF EXISTS medical_certificates ADD COLUMN IF NOT EXISTS title varchar(300);
ALTER TABLE IF EXISTS medical_certificates ADD COLUMN IF NOT EXISTS valid_from date;
ALTER TABLE IF EXISTS medical_certificates ADD COLUMN IF NOT EXISTS document_number varchar(100);
ALTER TABLE IF EXISTS medical_certificates ADD COLUMN IF NOT EXISTS status varchar(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE IF EXISTS medical_certificates ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
UPDATE medical_certificates SET valid_from = issued_at::date WHERE valid_from IS NULL;

ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS file_name varchar(300);
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS mime_type varchar(120);
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS file_size bigint;
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_visit_diagnoses_status ON visit_diagnoses(status);
CREATE INDEX IF NOT EXISTS idx_visit_diagnoses_created_at ON visit_diagnoses(created_at);
CREATE INDEX IF NOT EXISTS idx_prescriptions_status ON prescriptions(status);
CREATE INDEX IF NOT EXISTS idx_prescriptions_created_at ON prescriptions(created_at);
CREATE INDEX IF NOT EXISTS idx_medication_prescriptions_status ON medication_prescriptions(status);
CREATE INDEX IF NOT EXISTS idx_medication_prescriptions_created_at ON medication_prescriptions(created_at);
CREATE INDEX IF NOT EXISTS idx_lab_results_status ON lab_results(status);
CREATE INDEX IF NOT EXISTS idx_lab_results_created_at ON lab_results(created_at);
CREATE INDEX IF NOT EXISTS idx_referrals_status ON referrals(status);
CREATE INDEX IF NOT EXISTS idx_referrals_created_at ON referrals(created_at);
CREATE INDEX IF NOT EXISTS idx_medical_certificates_status ON medical_certificates(status);
CREATE INDEX IF NOT EXISTS idx_medical_documents_created_at ON medical_documents(created_at);
CREATE INDEX IF NOT EXISTS idx_medical_visits_created_at ON medical_visits(created_at);
CREATE INDEX IF NOT EXISTS idx_medical_visits_updated_at ON medical_visits(updated_at);
