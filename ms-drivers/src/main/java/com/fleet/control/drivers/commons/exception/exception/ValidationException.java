package com.fleet.control.drivers.commons.exception.exception;

/** Raised for invalid input detected manually in services (maps to 400 with details). */
public class ValidationException extends RuntimeException {
  private final String field;
  private final String reason;

  /**
   * Creates the validation error for a single field.
   *
   * @param field invalid field name
   * @param reason why the value is rejected
   */
  public ValidationException(String field, String reason) {
    super(reason);
    this.field = field;
    this.reason = reason;
  }

  /** Returns the invalid field name. */
  public String getField() {
    return field;
  }

  /** Returns why the value is rejected. */
  public String getReason() {
    return reason;
  }
}
