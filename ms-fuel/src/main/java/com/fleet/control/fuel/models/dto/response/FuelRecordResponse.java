package com.fleet.control.fuel.models.dto.response;

import com.fleet.control.fuel.models.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Refuel detail returned by registration and history endpoints. */
@Schema(description = "Refuel detail returned by registration and history endpoints.")
public record FuelRecordResponse(
    @Schema(description = "Refuel identifier") UUID id,
    @Schema(description = "Vehicle identifier") UUID vehicleId,
    @Schema(description = "Refuel date and time (UTC)", example = "2026-10-06T12:15:00Z")
        Instant refueledAt,
    @Schema(description = "Fuel type, taken from the vehicle", example = "DIESEL")
        FuelType fuelType,
    @Schema(description = "Liters refueled", example = "58.40") BigDecimal liters,
    @Schema(description = "Price per liter in EUR", example = "1.62") BigDecimal pricePerLiter,
    @Schema(description = "Total cost in EUR", example = "94.61") BigDecimal totalCost,
    @Schema(description = "Odometer at refuel time in kilometers", example = "45572")
        Integer odometerKm,
    @Schema(description = "Whether the tank was filled", example = "true") Boolean fullTank,
    @Schema(description = "Gas station", example = "Repsol A-3 km 112") String station,
    @Schema(
            description = "Computed consumption in L/100 km (null if no previous full tank)",
            example = "11.30")
        BigDecimal consumptionL100km) {}
