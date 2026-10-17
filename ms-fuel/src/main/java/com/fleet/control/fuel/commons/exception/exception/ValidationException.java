package com.fleet.control.fuel.commons.exception.exception;

/** Field validation failure mapped to 400 with details. */
public class ValidationException extends RuntimeException {
  private final String field;
  private final String reason;

  /** Creates the failure for the given field and reason. */
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
