package com.fleet.control.auth.common.exceptions;

public class DuplicateUsernameException extends RuntimeException {

  public DuplicateUsernameException(String message) {
    super(message);
  }
}
