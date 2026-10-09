package com.fleet.control.vehicles.common.models.dtos.response;

import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import com.fleet.control.vehicles.common.models.enums.VehicleType;

import java.time.Instant;
import java.util.UUID;

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
        Instant updatedAt

) {
}
