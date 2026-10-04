package com.fleet.control.auth.models.dto.exceptions.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "Standard API error structure")
public record ErrorResponse(
    @Schema(description = "Machine-readable error code", example = "USER_ALREADY_EXISTS")
        String error,
    @Schema(description = "Error description", example = "A user with that email already exists")
        String message,
    @Schema(description = "Error timestamp", example = "2025-01-15T10:30:00Z") Instant timestamp) {}
