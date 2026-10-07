package com.fleet.control.auth.integration.path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fleet.control.auth.common.path.ApiPaths;
import org.junit.jupiter.api.Test;

/** Verifies the public route paths match the API contract. */
class ApiPathTest {

  @Test
  void endpointsMatchContract() {
    assertEquals("/api/auth/register", ApiPaths.REGISTER_ENDPOINT);
    assertEquals("/api/auth/login", ApiPaths.LOGIN_ENDPOINT);
    assertEquals("/api/auth/validate", ApiPaths.VALIDATE_ENDPOINT);
  }

  @Test
  void publicEndpointsCoverAuthenticationFlow() {
    assertArrayEquals(
        new String[] {
          ApiPaths.REGISTER_ENDPOINT, ApiPaths.LOGIN_ENDPOINT, ApiPaths.VALIDATE_ENDPOINT
        },
        ApiPaths.PUBLIC_AUTH_ENDPOINTS);
  }
}
