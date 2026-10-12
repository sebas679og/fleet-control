package com.fleet.control.vehicles.exceptions.enums;

/** Error codes for vehicle operations. */
public enum ErrorCode {
  VALIDATION_ERROR,
  VEHICLE_NOT_FOUND,
  VEHICLE_ALREADY_EXISTS,
  INVALID_STATUS_TRANSITION,
  INVALID_ODOMETER,
  UNAUTHORIZED,
  TOKEN_EXPIRED,
  FORBIDDEN,
  SERVICE_UNAVAILABLE,
  INTERNAL_ERROR
}
