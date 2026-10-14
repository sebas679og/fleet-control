package com.fleet.control.drivers.commons.exception.exception;

import com.fleet.control.drivers.commons.exception.ApiException;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Raised when another driver already uses the requested license number. */
public class DriverAlreadyExistsException extends ApiException {
  /**
   * Creates the error for the duplicated license.
   *
   * @param licenseNumber conflicting license number
   */
  public DriverAlreadyExistsException(String licenseNumber) {
    super(
        ErrorCode.DRIVER_ALREADY_EXISTS,
        HttpStatus.CONFLICT,
        "Driver already exists with license number: " + licenseNumber);
  }
}
