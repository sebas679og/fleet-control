package com.fleet.control.auth.service;

import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import io.jsonwebtoken.Claims;

/** Contract for HS256 JWT issuance and validation. */
public interface JwtService {

  /**
   * Issues a signed JWT carrying the user identity and role.
   *
   * @param email the user email used as subject
   * @param userId the user identifier claim
   * @param username the username claim
   * @param role the role claim
   * @return the token response with user data
   */
  TokenResponse generateToken(String email, String userId, String username, String role);

  /**
   * Validates a token without throwing, reporting the outcome in the payload.
   *
   * @param token the JWT to validate
   * @return a valid payload, or an invalid one carrying the error code
   */
  TokenPayload validateToken(String token);

  /**
   * Parses a token and returns its claims.
   *
   * @param token the JWT to parse
   * @return the token claims
   */
  Claims getClaims(String token);

  /**
   * Checks whether a token is expired or unparsable.
   *
   * @param token the JWT to check
   * @return true when expired or invalid
   */
  boolean isExpired(String token);

  /**
   * Extracts the role claim from a token.
   *
   * @param token the JWT to read
   * @return the role claim
   */
  String extractRole(String token);

  /**
   * Extracts the subject (email) from a token.
   *
   * @param token the JWT to read
   * @return the token subject
   */
  String extractEmail(String token);
}
