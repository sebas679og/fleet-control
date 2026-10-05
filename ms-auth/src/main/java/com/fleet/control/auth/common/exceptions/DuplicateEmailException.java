package com.fleet.control.auth.common.exceptions;

/** Thrown when a registration email is already in use. */
public class DuplicateEmailException extends RuntimeException {

  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the conflict
   */
  public DuplicateEmailException(String message) {
    super(message);
  }
}
