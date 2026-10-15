package com.fleet.control.routes.models.dtos.response;

import com.fleet.control.routes.models.enums.RouteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RouteResponse(
    UUID id,
    UUID vehicleId,
    UUID driverId,
    String origin,
    String destination,
    Instant plannedStart,
    Integer estimatedDurationMin,
    BigDecimal plannedDistanceKm,
    RouteStatus status,
    Instant startedAt,
    Instant endedAt,
    Integer startOdometerKm,
    Integer endOdometerKm,
    BigDecimal actualDistanceKm,
    String notes,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt) {}
