package com.fleet.control.fuel.commons.exception.exception;

import com.fleet.control.fuel.commons.exception.ApiException;
import com.fleet.control.fuel.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Unknown vehicle mapped to 404. */
public class VehicleNotFoundException extends ApiException {
  public VehicleNotFoundException(UUID id) {
    super(ErrorCode.VEHICLE_NOT_FOUND, HttpStatus.NOT_FOUND, "Vehicle not found: " + id);
  }
}
