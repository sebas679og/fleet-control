package com.fleet.control.vehicles.common.exceptions.dto;

import com.fleet.control.vehicles.common.exceptions.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Error response payload. */
@Schema(description = "Error response payload")
public record ErrorResponse(
    String error,
    String message,
    List<Detail> details, // only for VALIDATION_ERROR, null otherwise
    String service, // only for SERVICE_UNAVAILABLE, null otherwise
    Instant timestamp) {
  /** Field-level validation detail. */
  @Schema(description = "Field-level validation detail")
  public record Detail(String field, String reason) {}

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.name(), message, null, null, Instant.now());
  }

  public static ErrorResponse of(ErrorCode code, String message, List<Detail> details) {
    return new ErrorResponse(code.name(), message, details, null, Instant.now());
  }
}
