package com.fleet.control.routes.models.dtos.response;

import com.fleet.control.routes.models.enums.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Route detail returned by planning, execution and history endpoints. */
@Schema(description = "Route detail returned by planning, execution and history endpoints.")
public record RouteResponse(
    @Schema(description = "Route identifier") UUID id,
    @Schema(description = "Vehicle identifier") UUID vehicleId,
    @Schema(description = "Driver identifier") UUID driverId,
    @Schema(description = "Route origin", example = "Madrid") String origin,
    @Schema(description = "Route destination", example = "Barcelona") String destination,
    @Schema(description = "Planned start (UTC)", example = "2026-11-01T08:00:00Z")
        Instant plannedStart,
    @Schema(description = "Estimated duration in minutes", example = "240")
        Integer estimatedDurationMin,
    @Schema(description = "Planned distance in kilometers", example = "620.50")
        BigDecimal plannedDistanceKm,
    @Schema(description = "Route status", example = "PLANNED") RouteStatus status,
    @Schema(description = "Actual start (UTC)") Instant startedAt,
    @Schema(description = "Actual end (UTC)") Instant endedAt,
    @Schema(description = "Odometer at start in kilometers") Integer startOdometerKm,
    @Schema(description = "Odometer at end in kilometers") Integer endOdometerKm,
    @Schema(description = "Actual distance traveled in kilometers") BigDecimal actualDistanceKm,
    @Schema(description = "Completion notes") String notes,
    @Schema(description = "Creator user identifier") UUID createdBy,
    @Schema(description = "Creation timestamp") Instant createdAt,
    @Schema(description = "Last update timestamp") Instant updatedAt) {}
