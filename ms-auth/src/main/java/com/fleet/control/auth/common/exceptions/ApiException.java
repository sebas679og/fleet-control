package com.fleet.control.auth.common.exceptions;

import com.fleet.control.auth.common.exceptions.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Base exception for auth domain errors. Mirrors the ms-drivers {@code ApiException} contract. */
public class ApiException extends RuntimeException {

  private final ErrorCode code;
  private final HttpStatus status;

  /**
   * Creates a domain error with its machine-readable code and HTTP status.
   *
   * @param code machine-readable error code
   * @param status HTTP status to respond with
   * @param message human-readable description
   */
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
