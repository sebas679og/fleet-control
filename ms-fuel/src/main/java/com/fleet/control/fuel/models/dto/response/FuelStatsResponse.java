package com.fleet.control.fuel.models.dto.response;

import com.fleet.control.fuel.models.enums.Granularity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Time series of liters, cost and average consumption consumed by the dashboard. */
@Schema(description = "Time series of liters, cost and average consumption.")
public record FuelStatsResponse(
    @Schema(description = "Range start", example = "2026-09-07") LocalDate from,
    @Schema(description = "Range end", example = "2026-09-27") LocalDate to,
    @Schema(description = "Time bucket", example = "WEEK") Granularity granularity,
    @Schema(description = "Aggregated totals for the requested period") Totals totals,
    @Schema(description = "Liters, cost and consumption grouped by period") List<Point> series) {

  /** Aggregated totals for the requested period. */
  @Schema(description = "Aggregated totals for the requested period.")
  public record Totals(
      @Schema(description = "Total liters", example = "3090.00") BigDecimal liters,
      @Schema(description = "Total cost in EUR", example = "5005.80") BigDecimal cost,
      @Schema(description = "Average price per liter in EUR", example = "1.62")
          BigDecimal avgPricePerLiter,
      @Schema(description = "Average consumption in L/100 km", example = "11.00")
          BigDecimal avgL100km) {}

  /** Liters, cost and consumption of a single period. */
  @Schema(description = "Liters, cost and consumption of a single period.")
  public record Point(
      @Schema(description = "Period start date", example = "2026-09-07") LocalDate period,
      @Schema(description = "Total liters", example = "984.70") BigDecimal liters,
      @Schema(description = "Total cost in EUR", example = "1595.21") BigDecimal cost,
      @Schema(description = "Average price per liter in EUR", example = "1.62")
          BigDecimal avgPricePerLiter,
      @Schema(description = "Average consumption in L/100 km", example = "10.90")
          BigDecimal avgL100km) {}
}
