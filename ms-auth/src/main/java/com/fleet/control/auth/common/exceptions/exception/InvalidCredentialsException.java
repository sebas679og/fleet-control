package com.fleet.control.auth.common.exceptions.exception;

import com.fleet.control.auth.common.exceptions.ApiException;
import com.fleet.control.auth.common.exceptions.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Thrown when login credentials are invalid. */
public class InvalidCredentialsException extends ApiException {

  public InvalidCredentialsException() {
    super(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, "Invalid credentials");
  }

  /**
   * Creates the exception with a detail message.
   *
   * @param message human-readable description of the failure
   */
  public InvalidCredentialsException(String message) {
    super(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, message);
  }
}
