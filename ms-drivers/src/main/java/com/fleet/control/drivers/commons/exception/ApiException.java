package com.fleet.control.drivers.commons.exception;

import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Base class for controlled API errors with a machine-readable code and HTTP status. */
public class ApiException extends RuntimeException {
  private final ErrorCode code;
  private final HttpStatus status;

  /**
   * Creates a controlled API error.
   *
   * @param code machine-readable error code
   * @param status HTTP status to return
   * @param message human-readable description
   */
  public ApiException(ErrorCode code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  /** Returns the machine-readable error code. */
  public ErrorCode getCode() {
    return code;
  }

  /** Returns the HTTP status to return. */
  public HttpStatus getStatus() {
    return status;
  }
}
