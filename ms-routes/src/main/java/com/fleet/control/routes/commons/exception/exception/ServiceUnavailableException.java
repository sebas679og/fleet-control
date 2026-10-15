package com.fleet.control.routes.commons.exception.exception;

import com.fleet.control.routes.commons.exception.ApiException;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Raised when a downstream service (vehicles, drivers) is unavailable. */
public class ServiceUnavailableException extends ApiException {
  private final String service;

  /**
   * Creates the error for the unavailable downstream service.
   *
   * @param service downstream service name
   */
  public ServiceUnavailableException(String service) {
    super(
        ErrorCode.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        "Downstream service unavailable: " + service);
    this.service = service;
  }

  /** Returns the downstream service name. */
  public String getService() {
    return service;
  }
}
