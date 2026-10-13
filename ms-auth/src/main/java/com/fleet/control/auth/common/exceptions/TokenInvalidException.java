package com.fleet.control.auth.common.exceptions;

import org.springframework.http.HttpStatus;

/** Thrown when a JWT is invalid or expired during explicit validation. */
public class TokenInvalidException extends ApiException {

  /**
   * Creates the exception with a machine-readable code and a detail message.
   *
   * @param error business error identifier
   * @param message human-readable description of the failure
   */
  public TokenInvalidException(ErrorCode error, String message) {
    super(error, HttpStatus.UNAUTHORIZED, message);
  }

  /** Returns the machine-readable error identifier for the {@code TokenPayload} contract. */
  public String getError() {
    return getCode().name();
  }
}
