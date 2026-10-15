CREATE TABLE routes (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    origin VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    planned_start TIMESTAMPTZ NOT NULL,
    estimated_duration_min INTEGER NOT NULL,
    planned_distance_km NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    start_odometer_km INTEGER,
    ended_at TIMESTAMPTZ,
    end_odometer_km INTEGER,
    actual_distance_km NUMERIC(10, 2),
    notes VARCHAR(500),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_routes_vehicle_start ON routes (vehicle_id, planned_start);
CREATE INDEX idx_routes_driver_start ON routes (driver_id, planned_start);
