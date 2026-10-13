package com.fleet.control.drivers.commons.exception.exception;

import com.fleet.control.drivers.commons.exception.ApiException;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class DriverAlreadyExistsException extends ApiException {
  public DriverAlreadyExistsException(String licenseNumber) {
    super(
        ErrorCode.DRIVER_ALREADY_EXIST,
        HttpStatus.CONFLICT,
        "Driver already exists with license number: " + licenseNumber);
  }
}
