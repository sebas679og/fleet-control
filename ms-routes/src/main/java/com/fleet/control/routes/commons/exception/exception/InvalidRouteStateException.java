package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Raised on illegal route state transitions (start/complete out of order). */
public class InvalidRouteStateException extends ApiException {
  /**
   * Creates the error for the illegal transition.
   *
   * @param from current route status
   * @param to requested route status
   */
  public InvalidRouteStateException(String from, String to) {
    super(
        ErrorCode.INVALID_ROUTE_STATE,
        HttpStatus.CONFLICT,
        "Route transition from " + from + " to " + to + " is not allowed");
  }
}
