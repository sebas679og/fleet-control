package com.fleet.control.vehicles.exceptions;

import com.fleet.control.vehicles.exceptions.dto.ErrorResponse;
import com.fleet.control.vehicles.exceptions.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

public class ApiVehicleException extends RuntimeException{

    private final ErrorCode code;
    private final HttpStatus status;
    private final List<ErrorResponse.Detail> details;

    public ApiVehicleException(ErrorCode code, HttpStatus status, String message) {
            this(code, status, message, null);
    }

    public ApiVehicleException(ErrorCode code, HttpStatus status, String message,
            List<ErrorResponse.Detail> details) {
            super(message);
            this.code = code;
            this.status = status;
            this.details = details;
    }
    public ErrorCode getCode() {
        return code;
    }
    public HttpStatus getStatus() {
        return status;
    }
    public List<ErrorResponse.Detail> getDetails() {
        return details;
    }

    //404
    public static class VehicleNotFoundException extends ApiVehicleException {
        public VehicleNotFoundException(UUID id) {
            super(ErrorCode.VEHICLE_NOT_FOUND, HttpStatus.NOT_FOUND,
                    String.format("Vehicle with id %s not found", id));
        }
    }

    //409
    public static class VehicleAlreadyExistsException extends ApiVehicleException {
        public VehicleAlreadyExistsException(String plate) {
            super(ErrorCode.VEHICLE_ALREADY_EXISTS, HttpStatus.CONFLICT,
                    String.format("Vehicle with plate %s already exists", plate));
        }
    }

    //409
    public static class InvalidStatusTransitionException extends ApiVehicleException {
        public InvalidStatusTransitionException(String from, String to) {
            super(ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.CONFLICT,
                    String.format("Status transition from %s to %s is not allowed", from, to));
        }
    }

    //400
    public static class InvalidVehicleYearException extends ApiVehicleException {
        public InvalidVehicleYearException(Integer year, int min, int max) {
            super(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Request contains invalid fields",
                    List.of(new ErrorResponse.Detail("year",
                            String.format("year %s debe estar entre %s y %s", year, min, max))));
        }
    }

    //400
    public static class InvalidPaginationException extends ApiVehicleException {
        public InvalidPaginationException(String field, String reason) {
            super(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Request contains invalid fields",
                    List.of(new ErrorResponse.Detail(field, reason)));
        }
    }

    //409
    public static class InvalidOdometerException extends ApiVehicleException {
        public InvalidOdometerException(Integer value, Integer current) {
            super(ErrorCode.INVALID_ODOMETER, HttpStatus.CONFLICT,
                    String.format("Odometer value %s is lower than current %s", value, current));
        }
    }

}
