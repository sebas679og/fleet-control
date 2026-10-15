CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY,
    plate VARCHAR(20) NOT NULL UNIQUE,
    make VARCHAR(40) NOT NULL,
    model VARCHAR(40) NOT NULL,
    year INTEGER NOT NULL,
    tank_capacity_l INTEGER NOT NULL,
    odometer_km INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vehicles_status ON vehicles (status);
CREATE INDEX IF NOT EXISTS idx_vehicles_type ON vehicles (type);
