package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Raised when the referenced vehicle does not exist in the vehicle service. */
public class VehicleNotFoundException extends ApiException {
  /**
   * Creates the error for the missing vehicle.
   *
   * @param id requested vehicle identifier
   */
  public VehicleNotFoundException(UUID id) {
    super(ErrorCode.VEHICLE_NOT_FOUND, HttpStatus.NOT_FOUND, "Vehicle not found: " + id);
  }
}
