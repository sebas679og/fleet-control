package com.fleet.control.routes.models.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompleteRouteRequest(
    @NotNull(message = "La distancia real es obligatoria")
        @Positive(message = "La distancia real debe ser mayor que 0")
        BigDecimal actualDistanceKm,
    @Size(max = 500) String notes) {}
