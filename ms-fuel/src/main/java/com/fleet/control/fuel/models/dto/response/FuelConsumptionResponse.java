package com.fleet.control.fuel.models.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Aggregated consumption per vehicle over a period, ordered by cost descending. */
@Schema(
    description = "Aggregated consumption per vehicle over a period, ordered by cost descending.")
public record FuelConsumptionResponse(
    @Schema(description = "Range start", example = "2026-09-01") LocalDate from,
    @Schema(description = "Range end", example = "2026-09-30") LocalDate to,
    @Schema(description = "Consumption grouped by vehicle") List<VehicleConsumption> vehicles) {

  /** Consumption of a single vehicle in the requested period. */
  @Schema(description = "Consumption of a single vehicle in the requested period.")
  public record VehicleConsumption(
      @Schema(description = "Vehicle identifier") UUID vehicleId,
      @Schema(description = "Vehicle plate", example = "4821-KDF") String plate,
      @Schema(description = "Vehicle type", example = "VAN") String type,
      @Schema(description = "Total liters", example = "690.30") BigDecimal liters,
      @Schema(description = "Total cost in EUR", example = "1118.29") BigDecimal cost,
      @Schema(description = "Distance in kilometers", example = "6310") Integer distanceKm,
      @Schema(description = "Average consumption in L/100 km", example = "10.90")
          BigDecimal avgL100km) {}
}
