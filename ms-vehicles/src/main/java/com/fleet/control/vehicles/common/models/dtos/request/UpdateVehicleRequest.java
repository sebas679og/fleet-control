package com.fleet.control.vehicles.common.models.dtos.request;

import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request payload to update a vehicle. */
public record UpdateVehicleRequest(
    @NotBlank(message = "make must not be blank")
        @Size(max = 40, message = "make must be at most 40 characters")
        String make,
    @NotBlank(message = "model must not be blank")
        @Size(max = 40, message = "model must be at most 40 characters")
        String model,
    @NotNull(message = "year must not be null")
        @Min(value = 1990, message = "year must be 1990 or later")
        Integer year,
    @NotNull(message = "type must not be null") VehicleType type,
    @NotNull(message = "fuelType must not be null") FuelType fuelType,
    @NotNull(message = "tankCapacityL must not be null")
        @Positive(message = "tankCapacityL must be greater than 0")
        Integer tankCapacityL) {}
