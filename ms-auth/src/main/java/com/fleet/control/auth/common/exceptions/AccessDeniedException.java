package com.fleet.control.auth.common.exceptions;

/** Thrown when an authenticated user lacks permission for the requested operation. */
public class AccessDeniedException extends RuntimeException {
  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the denial
   */
  public AccessDeniedException(String message) {
    super(message);
  }
}
