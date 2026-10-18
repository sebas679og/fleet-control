package com.fleet.control.fuel.models.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload to register a refuel. The fuel type is taken from the vehicle. */
@Schema(description = "Payload to register a refuel. The fuel type is taken from the vehicle.")
public record CreateFuelRecordRequest(
    @Schema(description = "Vehicle identifier", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        @NotNull(message = "")
        UUID vehicleId,
    @Schema(description = "Refuel date and time (UTC)", example = "2026-10-06T12:15:00Z")
        @NotNull(message = "")
        Instant refueledAt,
    @Schema(description = "Liters refueled", example = "58.40")
        @NotNull(message = "")
        @Positive(message = "")
        BigDecimal liters,
    @Schema(description = "Price per liter in EUR", example = "1.62")
        @NotNull(message = "")
        @Positive(message = "")
        BigDecimal pricePerLiter,
    @Schema(description = "Odometer at refuel time in kilometers", example = "45572")
        @NotNull(message = "")
        @PositiveOrZero(message = "")
        Integer odometerKm,
    @Schema(description = "Whether the tank was filled", example = "true") @NotNull(message = "")
        Boolean fullTank,
    @Schema(description = "Gas station", example = "Repsol A-3 km 112") @Size(max = 120)
        String station) {}
