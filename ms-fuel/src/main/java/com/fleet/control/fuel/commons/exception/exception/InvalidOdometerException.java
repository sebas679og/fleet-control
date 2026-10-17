package com.fleet.control.fuel.commons.exception.exception;

import com.fleet.control.fuel.commons.exception.ApiException;
import com.fleet.control.fuel.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Odometer rollback mapped to 409. */
public class InvalidOdometerException extends ApiException {
  /** Creates the failure with the last recorded and the provided odometer. */
  public InvalidOdometerException(int current, int provided) {
    super(
        ErrorCode.INVALID_ODOMETER,
        HttpStatus.CONFLICT,
        "Provided odometer (" + provided + ") is less than last recorded (" + current + ")");
  }
}
