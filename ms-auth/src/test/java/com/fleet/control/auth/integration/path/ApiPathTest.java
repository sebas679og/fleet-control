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
        new String[] {ApiPaths.REGISTER_ENDPOINT, ApiPaths.LOGIN_ENDPOINT},
        ApiPaths.PUBLIC_AUTH_ENDPOINTS);
  }

  @Test
  void validateEndpointIsInternal() {
    assertEquals("/api/auth/validate", ApiPaths.VALIDATE_ENDPOINT);
    for (String endpoint : ApiPaths.PUBLIC_AUTH_ENDPOINTS) {
      if (ApiPaths.VALIDATE_ENDPOINT.equals(endpoint)) {
        throw new AssertionError("Validate endpoint must not be public");
      }
    }
  }
}
