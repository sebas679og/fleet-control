package com.fleet.control.drivers.commons.exception.exception;

import com.fleet.control.drivers.commons.exception.ApiException;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class DriverNotFoundException extends ApiException {
  public DriverNotFoundException(UUID id) {
    super(ErrorCode.DRIVER_NOT_FOUND, HttpStatus.NOT_FOUND, "Driver Not Exist " + id);
  }
}
