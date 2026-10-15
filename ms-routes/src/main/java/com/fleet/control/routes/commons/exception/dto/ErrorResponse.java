package com.fleet.control.routes.commons.exception.dto;

import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Common API error structure returned for every controlled failure. */
@Schema(description = "Error response payload")
public record ErrorResponse(
    String error, String message, List<Detail> details, String service, Instant timestamp) {
  /** Single field validation failure, only present for {@code VALIDATION_ERROR}. */
  @Schema(description = "Field-level validation detail")
  public record Detail(String field, String reason) {}

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.name(), message, null, null, Instant.now());
  }

  public static ErrorResponse of(ErrorCode code, String message, List<Detail> details) {
    return new ErrorResponse(code.name(), message, details, null, Instant.now());
  }

  public static ErrorResponse of(ErrorCode code, String message, String service) {
    return new ErrorResponse(code.name(), message, null, service, Instant.now());
  }
}
