package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Raised when the vehicle is not AVAILABLE at planning or start time. */
public class VehicleNotAvailableException extends ApiException {
  /**
   * Creates the error for the unavailable vehicle.
   *
   * @param id conflicting vehicle identifier
   */
  public VehicleNotAvailableException(UUID id) {
    super(ErrorCode.VEHICLE_NOT_AVAILABLE, HttpStatus.CONFLICT, "Vehicle is not available: " + id);
  }
}
