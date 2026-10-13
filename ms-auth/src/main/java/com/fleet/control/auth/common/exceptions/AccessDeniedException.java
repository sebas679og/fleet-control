package com.fleet.control.auth.common.exceptions;

import org.springframework.http.HttpStatus;

/** Thrown when an authenticated user lacks permission for the requested operation. */
public class AccessDeniedException extends ApiException {

  public AccessDeniedException() {
    super(
        ErrorCode.FORBIDDEN,
        HttpStatus.FORBIDDEN,
        "Access denied: insufficient permissions");
  }

  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the denial
   */
  public AccessDeniedException(String message) {
    super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
  }
}
