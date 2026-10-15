package com.fleet.control.auth.common.exceptions.exception;

import com.fleet.control.auth.common.exceptions.ApiException;
import com.fleet.control.auth.common.exceptions.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Thrown when a registration email is already in use. */
public class DuplicateEmailException extends ApiException {

  /** Creates the exception with the default conflict message. */
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
