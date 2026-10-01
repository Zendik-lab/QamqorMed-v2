-- QamqorMed family patient model migration.
-- Apply after medical_structure_postgresql.sql when production uses ddl-auto=validate.

ALTER TABLE users ADD COLUMN IF NOT EXISTS email varchar(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth date;
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender varchar(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS iin varchar(12);
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_region_id bigint;
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_city_id bigint;
CREATE INDEX IF NOT EXISTS idx_users_preferred_region_id ON users(preferred_region_id);
CREATE INDEX IF NOT EXISTS idx_users_preferred_city_id ON users(preferred_city_id);

CREATE TABLE IF NOT EXISTS dependents (
    id bigserial PRIMARY KEY,
    owner_id bigint NOT NULL REFERENCES users(id),
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    middle_name varchar(100),
    date_of_birth date NOT NULL,
    gender varchar(30),
    iin varchar(12),
    relationship varchar(20) NOT NULL,
    phone varchar(30),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT uk_dependents_owner_iin UNIQUE (owner_id, iin)
);
CREATE INDEX IF NOT EXISTS idx_dependents_owner_id ON dependents(owner_id);
CREATE INDEX IF NOT EXISTS idx_dependents_active ON dependents(is_active);

CREATE TABLE IF NOT EXISTS appointments (
    id bigserial PRIMARY KEY,
    booked_by_user_id bigint NOT NULL REFERENCES users(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_profile_id bigint NOT NULL REFERENCES doctor_profiles(id),
    appointment_at timestamp without time zone NOT NULL,
    status varchar(30) NOT NULL,
    note varchar(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_appointments_booked_by ON appointments(booked_by_user_id);
CREATE INDEX IF NOT EXISTS idx_appointments_dependent ON appointments(dependent_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments(doctor_profile_id);
CREATE INDEX IF NOT EXISTS idx_appointments_at ON appointments(appointment_at);

CREATE TABLE IF NOT EXISTS time_slots (
    id bigserial PRIMARY KEY,
    doctor_profile_id bigint NOT NULL REFERENCES doctor_profiles(id),
    starts_at timestamp without time zone NOT NULL,
    ends_at timestamp without time zone NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT uk_time_slots_doctor_start UNIQUE (doctor_profile_id, starts_at)
);
CREATE INDEX IF NOT EXISTS idx_time_slots_doctor_start ON time_slots(doctor_profile_id, starts_at);
CREATE INDEX IF NOT EXISTS idx_time_slots_active ON time_slots(is_active);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS time_slot_id bigint REFERENCES time_slots(id);

CREATE TABLE IF NOT EXISTS medical_records (
    id bigserial PRIMARY KEY,
    appointment_id bigint NOT NULL UNIQUE REFERENCES appointments(id),
    patient_user_id bigint NOT NULL REFERENCES users(id),
    dependent_id bigint REFERENCES dependents(id),
    doctor_user_id bigint NOT NULL REFERENCES users(id),
    diagnosis varchar(2000),
    recommendations varchar(4000),
    prescriptions varchar(4000),
    referrals varchar(2000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_medical_records_owner ON medical_records(patient_user_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_dependent ON medical_records(dependent_id);

CREATE TABLE IF NOT EXISTS nurse_intakes (
    id bigserial PRIMARY KEY,
    appointment_id bigint NOT NULL UNIQUE REFERENCES appointments(id),
    nurse_user_id bigint NOT NULL REFERENCES users(id),
    temperature numeric(5,2),
    blood_pressure varchar(30),
    pulse integer,
    weight numeric(6,2),
    height numeric(6,2),
    notes varchar(2000),
    created_at timestamp with time zone NOT NULL
);
