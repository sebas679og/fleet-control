CREATE TABLE IF NOT EXISTS fuel_records (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    refuel_at TIMESTAMPTZ NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    liters NUMERIC(10, 2) NOT NULL,
    price_per_liter NUMERIC(10, 2) NOT NULL,
    total_cost NUMERIC NOT NULL,
    odometer_km INTEGER NOT NULL,
    full_tank BOOLEAN NOT NULL,
    station VARCHAR(120),
    consumption_l100km NUMERIC(10, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fuel_records_vehicle ON fuel_records (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_fuel_records_refueled ON fuel_records (refuel_at);
