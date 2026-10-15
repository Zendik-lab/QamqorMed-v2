-- Repair legacy PostgreSQL installations whose old appointment CHECK rejects
-- BOOKED. This is a separate migration so already-applied V2 checksums stay
-- valid. Existing legacy values are normalized without deleting appointments.

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;
UPDATE appointments SET status = 'BOOKED' WHERE status = 'SCHEDULED';
UPDATE appointments SET status = 'ARRIVED' WHERE status = 'CHECKED_IN';
UPDATE appointments SET status = 'IN_APPOINTMENT' WHERE status IN ('DOCTOR_IN_PROGRESS', 'IN_PROGRESS');
ALTER TABLE appointments ALTER COLUMN status SET DEFAULT 'BOOKED';
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check CHECK (status IN (
    'BOOKED', 'CONFIRMED', 'ARRIVED', 'PREPARING', 'READY',
    'IN_APPOINTMENT', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
));
