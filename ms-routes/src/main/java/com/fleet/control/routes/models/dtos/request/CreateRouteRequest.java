package com.fleet.control.routes.models.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload to plan a route assigning a vehicle and a driver. */
@Schema(description = "Payload to plan a route assigning a vehicle and a driver.")
public record CreateRouteRequest(
    @Schema(description = "Vehicle identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El vehículo es obligatorio")
        UUID vehicleId,
    @Schema(description = "Driver identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "El conductor es obligatorio")
        UUID driverId,
    @Schema(description = "Route origin", example = "Madrid")
        @NotBlank(message = "El origen es obligatorio")
        @Size(max = 200)
        String origin,
    @Schema(description = "Route destination", example = "Barcelona")
        @NotBlank(message = "El destino es obligatorio")
        @Size(max = 200)
        String destination,
    @Schema(description = "Planned start (UTC)", example = "2026-11-01T08:00:00Z")
        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant plannedStart,
    @Schema(description = "Estimated duration in minutes", example = "240")
        @NotNull(message = "La duración estimada es obligatoria")
        @Positive(message = "La duración debe ser mayor que 0")
        Integer estimatedDurationMin,
    @Schema(description = "Planned distance in kilometers", example = "620.50")
        @NotNull(message = "La distancia planificada es obligatoria")
        @Positive(message = "La distancia debe ser mayor que 0")
        BigDecimal plannedDistanceKm) {}
