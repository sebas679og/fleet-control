package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Raised when the referenced driver does not exist in the driver service. */
public class DriverNotFoundException extends ApiException {
  /**
   * Creates the error for the missing driver.
   *
   * @param id requested driver identifier
   */
  public DriverNotFoundException(UUID id) {
    super(ErrorCode.DRIVER_NOT_FOUND, HttpStatus.NOT_FOUND, "Driver not found: " + id);
  }
}
