package com.fleet.control.auth.common.exceptions;

/** Thrown when a registration username is already in use. */
public class DuplicateUsernameException extends RuntimeException {

  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the conflict
   */
  public DuplicateUsernameException(String message) {
    super(message);
  }
}
