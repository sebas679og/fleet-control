package com.fleet.control.routes.commons.exception.enums;

/** Machine-readable error codes returned in the common error contract. */
public enum ErrorCode {
  VALIDATION_ERROR,
  ROUTE_NOT_FOUND,
  VEHICLE_NOT_FOUND,
  DRIVER_NOT_FOUND,
  VEHICLE_NOT_AVAILABLE,
  DRIVER_NOT_ELIGIBLE,
  ROUTE_OVERLAP,
  INVALID_ROUTE_STATE,
  UNAUTHORIZED,
  TOKEN_EXPIRED,
  FORBIDDEN,
  SERVICE_UNAVAILABLE,
  INTERNAL_ERROR,
}
