package com.fleet.control.auth.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.service.impl.JwtServiceImpl;
import org.junit.jupiter.api.Test;

/** Unit tests for JWT issuance and its main edge cases. */
class JwtServiceImplTest {

  private static final String SECRET = "test-only-secret-with-at-least-32-characters-123456";

  @Test
  void generateTokenIssuesBearerTokenWithNormalizedRole() {
    JwtServiceImpl jwtService = new JwtServiceImpl(SECRET, 3600000L);

    TokenResponse response =
        jwtService.generateToken(
            "john.doe@example.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "john_doe",
            "ROLE_MANAGER");

    assertEquals("Bearer", response.tokenType());
    assertEquals(3600L, response.expiresIn());
    assertEquals("MANAGER", response.role());

    TokenPayload payload = jwtService.validateToken(response.token());
    assertTrue(payload.valid());
    assertEquals("550e8400-e29b-41d4-a716-446655440000", payload.userId());
  }

  @Test
  void constructorRejectsShortSecret() {
    assertThrows(IllegalArgumentException.class, () -> new JwtServiceImpl("short", 3600000L));
  }

  @Test
  void validateTokenReportsExpiredToken() {
    JwtServiceImpl jwtService = new JwtServiceImpl(SECRET, -1000L);

    TokenResponse response =
        jwtService.generateToken(
            "john.doe@example.com", "550e8400-e29b-41d4-a716-446655440000", "john_doe", "MANAGER");

    TokenPayload payload = jwtService.validateToken(response.token());

    assertEquals(false, payload.valid());
    assertEquals(ErrorCodes.TOKEN_EXPIRED, payload.error());
  }
}
