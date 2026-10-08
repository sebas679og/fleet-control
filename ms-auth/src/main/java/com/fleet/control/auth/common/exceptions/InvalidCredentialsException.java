package com.fleet.control.auth.common.exceptions;

/** Thrown when login credentials are invalid. */
public class InvalidCredentialsException extends RuntimeException {
  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the failure
   */
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
