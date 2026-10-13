package com.fleet.control.drivers.commons.exception.exception;

import com.fleet.control.drivers.commons.exception.ApiException;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class DriverAlreadyExistException extends ApiException {
    public DriverAlreadyExistException(String licenseNumber) {
        super(ErrorCode.DRIVER_ALREADY_EXIST, HttpStatus.CONFLICT,
                "Driver al ready exist with license number :" + licenseNumber);
    }
}
