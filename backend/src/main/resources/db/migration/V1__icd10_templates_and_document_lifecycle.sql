-- Additive migration for the existing QamqorMed schema. It assumes the base
-- medical structure has already been installed; no patient or visit data is
-- deleted or rewritten.

ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS title_ru varchar(500);
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS title_kk varchar(500);
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS title_en varchar(500);
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS chapter varchar(20);
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS parent_code varchar(20);
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS selectable boolean NOT NULL DEFAULT true;
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS source varchar(120);
ALTER TABLE IF EXISTS icd10_codes ADD COLUMN IF NOT EXISTS source_version varchar(80);
UPDATE icd10_codes SET selectable = true WHERE selectable IS NULL;
ALTER TABLE icd10_codes ALTER COLUMN selectable SET DEFAULT true;
ALTER TABLE icd10_codes ALTER COLUMN selectable SET NOT NULL;
UPDATE icd10_codes SET title_ru = COALESCE(title_ru, name) WHERE title_ru IS NULL;
CREATE INDEX IF NOT EXISTS idx_icd10_title_ru ON icd10_codes(title_ru);
CREATE INDEX IF NOT EXISTS idx_icd10_title_kk ON icd10_codes(title_kk);
CREATE INDEX IF NOT EXISTS idx_icd10_title_en ON icd10_codes(title_en);

ALTER TABLE IF EXISTS visit_diagnoses ADD COLUMN IF NOT EXISTS icd10_code_snapshot varchar(20);
ALTER TABLE IF EXISTS visit_diagnoses ADD COLUMN IF NOT EXISTS icd10_name_snapshot varchar(500);
UPDATE visit_diagnoses d SET icd10_code_snapshot = c.code, icd10_name_snapshot = COALESCE(c.title_ru, c.name)
FROM icd10_codes c WHERE d.icd10_code_id = c.id AND (d.icd10_code_snapshot IS NULL OR d.icd10_name_snapshot IS NULL);

ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS document_number varchar(80);
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS language varchar(5);
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS template_id bigint;
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS template_version_id bigint;
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS draft_content text;
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS finalized_content text;
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS approved_at timestamptz;
ALTER TABLE IF EXISTS medical_documents ADD COLUMN IF NOT EXISTS cancelled_at timestamptz;
UPDATE medical_documents SET status = 'APPROVED' WHERE status IS NULL;
ALTER TABLE IF EXISTS medical_documents ALTER COLUMN status SET DEFAULT 'APPROVED';
ALTER TABLE IF EXISTS medical_documents ALTER COLUMN status SET NOT NULL;
UPDATE medical_documents SET document_number = 'QMED-' || id WHERE document_number IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_medical_documents_number ON medical_documents(document_number);
CREATE INDEX IF NOT EXISTS idx_medical_documents_status ON medical_documents(status);

CREATE TABLE IF NOT EXISTS document_templates (
    id bigserial PRIMARY KEY,
    name varchar(200) NOT NULL,
    document_type varchar(30) NOT NULL,
    scope varchar(20) NOT NULL,
    owner_doctor_id bigint REFERENCES doctor_profiles(id),
    clinic_id bigint REFERENCES clinics(id),
    specialty_id bigint REFERENCES specialties(id),
    language varchar(5) NOT NULL,
    current_version_id bigint,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_document_templates_scope ON document_templates(scope);
CREATE INDEX IF NOT EXISTS idx_document_templates_type ON document_templates(document_type);
CREATE INDEX IF NOT EXISTS idx_document_templates_language ON document_templates(language);
CREATE INDEX IF NOT EXISTS idx_document_templates_specialty ON document_templates(specialty_id);
CREATE INDEX IF NOT EXISTS idx_document_templates_clinic ON document_templates(clinic_id);

CREATE TABLE IF NOT EXISTS document_template_versions (
    id bigserial PRIMARY KEY,
    template_id bigint NOT NULL REFERENCES document_templates(id),
    version_number integer NOT NULL,
    content text NOT NULL,
    required_fields varchar(2000),
    created_by bigint REFERENCES users(id),
    change_description varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_document_template_versions_number UNIQUE(template_id, version_number)
);
CREATE INDEX IF NOT EXISTS idx_document_template_versions_template ON document_template_versions(template_id);
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_document_templates_current_version') THEN
        ALTER TABLE document_templates ADD CONSTRAINT fk_document_templates_current_version FOREIGN KEY (current_version_id) REFERENCES document_template_versions(id);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_medical_documents_template') THEN
        ALTER TABLE medical_documents ADD CONSTRAINT fk_medical_documents_template FOREIGN KEY (template_id) REFERENCES document_templates(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_medical_documents_template_version') THEN
        ALTER TABLE medical_documents ADD CONSTRAINT fk_medical_documents_template_version FOREIGN KEY (template_version_id) REFERENCES document_template_versions(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS doctor_favorite_templates (
    id bigserial PRIMARY KEY,
    doctor_id bigint NOT NULL REFERENCES doctor_profiles(id),
    template_id bigint NOT NULL REFERENCES document_templates(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_doctor_favorite_template UNIQUE(doctor_id, template_id)
);
CREATE TABLE IF NOT EXISTS doctor_default_templates (
    id bigserial PRIMARY KEY,
    doctor_id bigint NOT NULL REFERENCES doctor_profiles(id),
    document_type varchar(30) NOT NULL,
    template_id bigint NOT NULL REFERENCES document_templates(id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_doctor_default_template_type UNIQUE(doctor_id, document_type)
);
CREATE TABLE IF NOT EXISTS document_audit_log (
    id bigserial PRIMARY KEY,
    document_id bigint NOT NULL REFERENCES medical_documents(id),
    actor_id bigint NOT NULL REFERENCES users(id),
    action varchar(40) NOT NULL,
    metadata varchar(2000),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_document_audit_document ON document_audit_log(document_id);
CREATE INDEX IF NOT EXISTS idx_document_audit_created ON document_audit_log(created_at);
