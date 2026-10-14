package com.fleet.control.auth.common.exceptions.exception;

import com.fleet.control.auth.common.exceptions.ApiException;
import com.fleet.control.auth.common.exceptions.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Thrown when a registration username is already in use. */
public class DuplicateUsernameException extends ApiException {

  /** Creates the exception with the default conflict message. */
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
