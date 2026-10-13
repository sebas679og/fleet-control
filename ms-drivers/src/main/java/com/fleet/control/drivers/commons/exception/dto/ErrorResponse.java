package com.fleet.control.drivers.commons.exception.dto;

import com.fleet.control.drivers.commons.exception.enums.ErrorCode;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        List<Detail> details,
        String service,
        Instant timestamp
) {
    public record Detail(String field, String reason) {}

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, null, null, Instant.now());
    }

    public static ErrorResponse of(ErrorCode code, String message, List<Detail> details) {
        return new ErrorResponse(code.name(), message, details, null, Instant.now());
    }
}