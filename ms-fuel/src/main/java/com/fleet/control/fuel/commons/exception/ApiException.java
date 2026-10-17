package com.fleet.control.fuel.commons.exception;

import com.fleet.control.fuel.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Base API exception carrying a machine-readable code and HTTP status. */
public class ApiException extends RuntimeException {
  private final ErrorCode code;
  private final HttpStatus status;

  /** Creates the exception with its error code, status and message. */
  public ApiException(ErrorCode code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public ErrorCode getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
