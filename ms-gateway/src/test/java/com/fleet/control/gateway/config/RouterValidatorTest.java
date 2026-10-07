package com.fleet.control.gateway.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

/** Unit tests for the secured-route check. */
class RouterValidatorTest {

  private final RouterValidator validator = new RouterValidator();

  @Test
  void openEndpointsAreNotSecured() {
    assertFalse(validator.isSecured(MockServerHttpRequest.get("/api/auth/login").build()));
    assertFalse(validator.isSecured(MockServerHttpRequest.get("/api/auth/register").build()));
  }

  @Test
  void otherRoutesAreSecured() {
    assertTrue(validator.isSecured(MockServerHttpRequest.get("/api/vehicles").build()));
  }
}
