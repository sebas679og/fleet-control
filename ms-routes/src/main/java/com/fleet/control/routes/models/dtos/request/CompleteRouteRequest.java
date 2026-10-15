package com.fleet.control.routes.models.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Payload to complete a route in progress. */
@Schema(description = "Payload to complete a route in progress.")
public record CompleteRouteRequest(
    @Schema(description = "Actual distance traveled in kilometers", example = "632.10")
        @NotNull(message = "La distancia real es obligatoria")
        @Positive(message = "La distancia real debe ser mayor que 0")
        BigDecimal actualDistanceKm,
    @Schema(description = "Completion notes", example = "Traffic delay near Zaragoza")
        @Size(max = 500)
        String notes) {}
