package com.fleet.control.vehicles.common.models.dtos.request;

import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import jakarta.validation.constraints.*;

public record CreateVehicleRequest(

        @NotBlank(message = "plate must not be blank")
        @Size(max = 20, message = "plate must be at most 20 characters")
        String plate,

        @NotBlank(message = "make must not be blank")
        @Size(max = 40, message = "make must be at most 40 characters")
        String make,

        @NotBlank(message = "model must not be blank")
        @Size(max = 40, message = "model must be at most 40 characters")
        String model,

        @NotNull(message = "year must not be null")
        @Min(value = 1990, message = "year must be 1990 or later")
        Integer year,

        @NotNull(message = "type must not be null")
        VehicleType type,

        @NotNull(message = "fuelType must not be null")
        FuelType fuelType,

        @NotNull(message = "tankCapacityL must not be null")
        @Positive(message = "tankCapacityL must be greater than 0")
        Integer tankCapacityL,

        @NotNull(message = "odometerKm must not be null")
        @PositiveOrZero(message = "odometerKm must be 0 or greater")
        Integer odometerKm


) {
}
