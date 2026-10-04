package com.fleet.control.auth.common.exceptions;

import lombok.Getter;

@Getter
public class TokenInvalidException extends RuntimeException {
  private final String error;
  private final String message;

  public TokenInvalidException(String error, String message) {
    super(message);
    this.error = error;
    this.message = message;
  }
}
