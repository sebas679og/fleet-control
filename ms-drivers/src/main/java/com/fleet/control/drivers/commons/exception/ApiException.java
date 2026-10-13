package com.fleet.control.drivers.commons.exception;

import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException{
    private final ErrorCode code;
    private final HttpStatus status;

    public ApiException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCode  getCode() {
        return code;
    }
    public HttpStatus getStatus() {
        return status;
    }


}
