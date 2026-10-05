package com.fleet.control.auth.common.exceptions;

import lombok.Getter;

/** Thrown when a JWT is invalid or expired during explicit validation. */
@Getter
public class TokenInvalidException extends RuntimeException {
  private final String error;
  private final String message;

  /**
   * Creates the exception with a machine-readable code and a detail message.
   *
   * @param error business error identifier
   * @param message human-readable description of the failure
   */
  public TokenInvalidException(String error, String message) {
    super(message);
    this.error = error;
    this.message = message;
  }
}
