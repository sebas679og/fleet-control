package com.fleet.control.vehicles.common.models.dtos.request;

import com.fleet.control.vehicles.common.models.enums.FuelType;
import com.fleet.control.vehicles.common.models.enums.VehicleType;
import jakarta.validation.constraints.*;

public record UpdateVehicleRequest(

        @NotBlank @Size(max = 50)
        String make,
        @NotBlank @Size(max = 50)
        String model,
        @NotNull @Min(1990)
        Integer year,
        @NotNull
        VehicleType type,
        @NotNull
        FuelType fuelType,
        @NotNull @Positive
        Integer tankCapacityL

) {
}
