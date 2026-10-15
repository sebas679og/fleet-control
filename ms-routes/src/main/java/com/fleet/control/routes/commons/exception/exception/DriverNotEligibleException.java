package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Raised when the driver is not ACTIVE or lacks a valid license category. */
public class DriverNotEligibleException extends ApiException {
  /**
   * Creates the error for the ineligible driver.
   *
   * @param id conflicting driver identifier
   */
  public DriverNotEligibleException(UUID id) {
    super(ErrorCode.DRIVER_NOT_ELIGIBLE, HttpStatus.CONFLICT, "Driver is not eligible: " + id);
  }
}
