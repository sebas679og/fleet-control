package com.fleet.control.auth.common.exceptions;

import org.springframework.http.HttpStatus;

/** Thrown when a registration username is already in use. */
public class DuplicateUsernameException extends ApiException {

  public DuplicateUsernameException() {
    super(
        ErrorCode.USER_ALREADY_EXISTS,
        HttpStatus.CONFLICT,
        "A user with that username already exists");
  }

  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the conflict
   */
  public DuplicateUsernameException(String message) {
    super(ErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT, message);
  }
}
