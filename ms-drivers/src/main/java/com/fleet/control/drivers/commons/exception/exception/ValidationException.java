package com.fleet.control.drivers.commons.exception.exception;

public class ValidationException extends RuntimeException {
  private final String field;
  private final String reason;

  public ValidationException(String field, String reason) {
    super(reason);
    this.field = field;
    this.reason = reason;
  }

  public String getField() {
    return field;
  }

  public String getReason() {
    return reason;
  }
}
