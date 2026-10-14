package com.fleet.control.vehicles.common.models.dtos.response;

import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Response payload with vehicle data. */
@Schema(description = "Vehicle data")
public record VehicleResponse(
    UUID id,
    String plate,
    String make,
    String model,
    Integer year,
    VehicleType type,
    FuelType fuelType,
    Integer tankCapacityL,
    Integer odometerKm,
    VehicleStatus status,
    Instant createdAt,
    Instant updatedAt) {}
