package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Raised when no route exists with the requested identifier. */
public class RouteNotFoundException extends ApiException {
  /**
   * Creates the error for the missing route.
   *
   * @param id requested route identifier
   */
  public RouteNotFoundException(UUID id) {
    super(ErrorCode.ROUTE_NOT_FOUND, HttpStatus.NOT_FOUND, "Route not found: " + id);
  }
}
