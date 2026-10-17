package com.fleet.control.fuel.commons.exception.dto;

import com.fleet.control.fuel.commons.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Standard API error structure. */
@Schema(description = "Standard API error structure")
public record ErrorResponse(
    @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR") String error,
    @Schema(description = "Error description", example = "Request contains invalid fields")
        String message,
    @Schema(description = "Field-level validation details (only for VALIDATION_ERROR)")
        List<Detail> details,
    @Schema(
            description = "Unresponsive service (only for SERVICE_UNAVAILABLE)",
            example = "ms-vehicles")
        String service,
    @Schema(description = "Error timestamp", example = "2026-10-05T10:30:00Z") Instant timestamp) {

  /** Single field validation failure. */
  @Schema(description = "Single field validation failure")
  public record Detail(
      @Schema(description = "Invalid field name", example = "liters") String field,
      @Schema(description = "Failure reason", example = "must be greater than 0") String reason) {}

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.name(), message, null, null, Instant.now());
  }

  public static ErrorResponse of(ErrorCode code, String message, List<Detail> details) {
    return new ErrorResponse(code.name(), message, details, null, Instant.now());
  }

  /** Builds a downstream-unavailable error naming the failing service. */
  public static ErrorResponse ofService(String service) {
    return new ErrorResponse(
        ErrorCode.SERVICE_UNAVAILABLE.name(),
        "Downstream service unavailable: " + service,
        null,
        service,
        Instant.now());
  }
}
