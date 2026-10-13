package com.fleet.control.auth.common.exceptions;

import org.springframework.http.HttpStatus;

/** Thrown when a registration email is already in use. */
public class DuplicateEmailException extends ApiException {

  public DuplicateEmailException() {
    super(
        ErrorCode.USER_ALREADY_EXISTS,
        HttpStatus.CONFLICT,
        "A user with that email already exists");
  }

  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the conflict
   */
  public DuplicateEmailException(String message) {
    super(ErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT, message);
  }
}
