package com.fleet.control.drivers.commons.exception.exception;

import com.fleet.control.drivers.commons.exception.ApiException;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DriverNotFoundException extends ApiException {
    public DriverNotFoundException(UUID id) {
        super(ErrorCode.DRIVER_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Driver Not Exist " + id);
    }
}