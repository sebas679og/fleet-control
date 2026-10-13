package com.fleet.control.drivers.commons.exception.dto;

import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import java.time.Instant;
import java.util.List;

/** Common API error structure returned for every controlled failure. */
public record ErrorResponse(
    String error, String message, List<Detail> details, String service, Instant timestamp) {
  /** Single field validation failure, only present for {@code VALIDATION_ERROR}. */
  public record Detail(String field, String reason) {}

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.name(), message, null, null, Instant.now());
  }

  public static ErrorResponse of(ErrorCode code, String message, List<Detail> details) {
    return new ErrorResponse(code.name(), message, details, null, Instant.now());
  }
}
