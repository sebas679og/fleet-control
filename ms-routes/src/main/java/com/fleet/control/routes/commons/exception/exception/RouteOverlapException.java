package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Raised when the planned window overlaps another route of the vehicle or driver. */
public class RouteOverlapException extends ApiException {
  /** Creates the overlap error. */
  public RouteOverlapException() {
    super(
        ErrorCode.ROUTE_OVERLAP,
        HttpStatus.CONFLICT,
        "Route overlaps with another route for the vehicle or driver");
  }
}
