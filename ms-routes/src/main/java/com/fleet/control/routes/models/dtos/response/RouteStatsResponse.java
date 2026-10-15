package com.fleet.control.routes.models.dtos.response;

import com.fleet.control.routes.models.enums.Granularity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Time series of completed routes consumed by the dashboard. */
@Schema(description = "Time series of completed routes consumed by the dashboard.")
public record RouteStatsResponse(
    @Schema(description = "Start date", example = "2026-10-01") LocalDate from,
    @Schema(description = "End date", example = "2026-10-31") LocalDate to,
    @Schema(description = "Time bucket", example = "DAY") Granularity granularity,
    @Schema(description = "Aggregated totals for the requested period") Totals totals,
    @Schema(description = "Completed routes grouped by period") List<Point> series) {
  /** Aggregated totals for the requested period. */
  @Schema(description = "Aggregated totals for the requested period.")
  public record Totals(
      @Schema(description = "Number of completed routes", example = "42") long routes,
      @Schema(description = "Total distance in kilometers", example = "12500.75")
          BigDecimal distanceKm) {}

  /** Completed routes grouped by period. */
  @Schema(description = "Completed routes grouped by period.")
  public record Point(
      @Schema(description = "Period start date", example = "2026-10-01") LocalDate period,
      @Schema(description = "Number of completed routes", example = "3") long routes,
      @Schema(description = "Total distance in kilometers", example = "980.25")
          BigDecimal distanceKm,
      @Schema(description = "Average duration in minutes", example = "210")
          Integer avgDurationMin) {}
}
