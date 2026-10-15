package com.fleet.control.routes.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateRouteRequest(
    @NotNull(message = "El vehículo es obligatorio") UUID vehicleId,
    @NotNull(message = "El conductor es obligatorio") UUID driverId,
    @NotBlank(message = "El origen es obligatorio") @Size(max = 200) String origin,
    @NotBlank(message = "El destino es obligatorio") @Size(max = 200) String destination,
    @NotNull(message = "La fecha de inicio es obligatoria") Instant plannedStart,
    @NotNull(message = "La duración estimada es obligatoria")
        @Positive(message = "La duración debe ser mayor que 0")
        Integer estimatedDurationMin,
    @NotNull(message = "La distancia planificada es obligatoria")
        @Positive(message = "La distancia debe ser mayor que 0")
        BigDecimal plannedDistanceKm) {}
