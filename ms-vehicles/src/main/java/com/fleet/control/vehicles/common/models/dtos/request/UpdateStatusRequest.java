package com.fleet.control.vehicles.common.models.dtos.request;

import com.fleet.control.vehicles.common.models.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStatusRequest(

        @NotNull(message = "status must not be null")
        VehicleStatus status,
        @PositiveOrZero(message = "odometerKm must be 0 or greater")
        Integer odometerKm

) {
}
