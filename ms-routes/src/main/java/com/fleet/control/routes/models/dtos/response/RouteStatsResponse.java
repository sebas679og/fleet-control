package com.fleet.control.routes.models.dtos.response;

import com.fleet.control.routes.models.enums.Granularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RouteStatsResponse(
    LocalDate from, LocalDate to, Granularity granularity, Totals totals, List<Point> series) {
  public record Totals(long routes, BigDecimal distanceKm) {}

  public record Point(
      LocalDate period, long routes, BigDecimal distanceKm, Integer avgDurationMin) {}
}
