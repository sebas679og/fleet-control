package com.fleet.control.auth.models.dto.exceptions.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** Standard API error structure. */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API error structure")
public record ErrorResponse(
    @Schema(description = "Machine-readable error code", example = "USER_ALREADY_EXISTS")
        String error,
    @Schema(description = "Error description", example = "A user with that email already exists")
        String message,
    @Schema(description = "Error timestamp", example = "2025-01-15T10:30:00Z") Instant timestamp,
    @Schema(description = "Field-level validation details (only for VALIDATION_ERROR)")
        List<Violation> details) {
  /** Single field validation failure. */
  @Schema(description = "Single field validation failure")
  public record Violation(
      @Schema(description = "Invalid field name", example = "password") String field,
      @Schema(
              description = "Failure reason",
              example = "must contain at least one uppercase letter")
          String reason) {}
}
