package com.fleet.control.auth.common.exceptions.exception;

/** Thrown manually from services for single-field validation failures. */
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
