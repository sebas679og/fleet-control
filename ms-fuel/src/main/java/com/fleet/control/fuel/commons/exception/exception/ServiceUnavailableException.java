package com.fleet.control.fuel.commons.exception.exception;

import com.fleet.control.fuel.commons.exception.ApiException;
import com.fleet.control.fuel.commons.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/** Downstream failure mapped to 503 with the failing service name. */
public class ServiceUnavailableException extends ApiException {
  private final String service;

  /** Creates the failure for the given downstream service. */
  public ServiceUnavailableException(String service) {
    super(
        ErrorCode.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        "Downstream service unavailable: " + service);
    this.service = service;
  }

  public String getService() {
    return service;
  }
}
