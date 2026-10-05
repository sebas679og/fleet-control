package com.fleet.control.auth.service.impl;

import com.fleet.control.auth.common.constants.JwtConstants;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.common.exceptions.ErrorMessages;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** HS256 JWT issuance and validation. */
@Service
public class JwtServiceImpl implements JwtService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtServiceImpl.class);
  private final SecretKey secretKey;
  private final long expirationTime;

  /**
   * Creates the JWT service with the configured secret and expiration.
   *
   * @param secret signing secret, at least 32 characters
   * @param expiration token lifetime in milliseconds
   */
  public JwtServiceImpl(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
    if (secret.getBytes().length < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 characters.");
    }
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationTime = expiration;
  }

  /**
   * Issues a signed JWT carrying the user identity and role.
   *
   * @param email the user email used as subject
   * @param userId the user identifier claim
   * @param username the username claim
   * @param role the role claim
   * @return the token response with user data
   */
  @Override
  public TokenResponse generateToken(String email, String userId, String username, String role) {

    Date now = new Date();
    Date expirationDate = new Date(now.getTime() + expirationTime);

    String normalizedRole =
        role.startsWith(JwtConstants.ROLE_PREFIX)
            ? role.substring(JwtConstants.ROLE_PREFIX.length())
            : role;

    String token =
        Jwts.builder()
            .subject(email)
            .claim(JwtConstants.CLAIM_USER_ID, userId)
            .claim(JwtConstants.CLAIM_USERNAME, username)
            .claim(JwtConstants.CLAIM_ROLE, normalizedRole)
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(secretKey)
            .compact();

    return TokenResponse.builder()
        .token(token)
        .tokenType(JwtConstants.TOKEN_TYPE_BEARER)
        .expiresIn(TimeUnit.MILLISECONDS.toSeconds(expirationTime))
        .userId(userId)
        .role(normalizedRole)
        .build();
  }

  /**
   * Validates a token without throwing, reporting the outcome in the payload.
   *
   * @param token the JWT to validate
   * @return a valid payload, or an invalid one carrying the error code
   */
  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: contract never throws.
  public TokenPayload validateToken(String token) {

    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
      if (claims.getExpiration().before(new Date())) {
        return new TokenPayload(
            false, null, null, null, ErrorCodes.TOKEN_EXPIRED, ErrorMessages.TOKEN_EXPIRED);
      }
      return new TokenPayload(
          true,
          claims.get(JwtConstants.CLAIM_USER_ID, String.class),
          claims.get(JwtConstants.CLAIM_USERNAME, String.class),
          claims.get(JwtConstants.CLAIM_ROLE, String.class),
          null,
          null);
    } catch (ExpiredJwtException e) {
      return new TokenPayload(
          false, null, null, null, ErrorCodes.TOKEN_EXPIRED, ErrorMessages.TOKEN_EXPIRED);
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Invalid JWT: {}", e.getMessage());
      }
      return new TokenPayload(
          false, null, null, null, ErrorCodes.INVALID_TOKEN, ErrorMessages.INVALID_TOKEN);
    }
  }

  /**
   * Parses a token and returns its claims.
   *
   * @param token the JWT to parse
   * @return the token claims
   */
  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: rewrapped below.
  public Claims getClaims(String token) {
    try {
      return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    } catch (Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "Error parsing JWT: {}, Cause: {}",
            e.getMessage(),
            e.getCause() != null ? e.getCause().getMessage() : "N/A");
      }
      throw new IllegalArgumentException("Invalid or expired JWT", e);
    }
  }

  /**
   * Checks whether a token is expired or unparsable.
   *
   * @param token the JWT to check
   * @return true when expired or invalid
   */
  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: any failure means expired.
  public boolean isExpired(String token) {
    try {
      return getClaims(token).getExpiration().before(new Date());
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Extracts the role claim from a token.
   *
   * @param token the JWT to read
   * @return the role claim
   */
  @Override
  public String extractRole(String token) {
    return getClaims(token).get(JwtConstants.CLAIM_ROLE, String.class);
  }

  /**
   * Extracts the subject (email) from a token.
   *
   * @param token the JWT to read
   * @return the token subject
   */
  @Override
  public String extractEmail(String token) {
    return getClaims(token).getSubject();
  }
}
