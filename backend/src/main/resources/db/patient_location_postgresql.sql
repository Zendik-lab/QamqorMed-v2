-- Patient primary location migration. Apply after the medical structure migration.
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_region_id bigint;
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_city_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_preferred_region') THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_preferred_region FOREIGN KEY (preferred_region_id) REFERENCES regions(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_preferred_city') THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_preferred_city FOREIGN KEY (preferred_city_id) REFERENCES cities(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_preferred_region_id ON users(preferred_region_id);
CREATE INDEX IF NOT EXISTS idx_users_preferred_city_id ON users(preferred_city_id);
