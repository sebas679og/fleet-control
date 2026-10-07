package com.fleet.control.auth.container;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.control.auth.integration.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Proves the US-03 security chain on a path with no handler: 401 means a gate rejected the request,
 * 404 means both gates passed and only the missing handler remains.
 */
class ProtectedPathSecurityIntegrationTest extends AbstractIntegrationTest {

  private static final String PROBE_PATH = "/internal/probe";

  @LocalServerPort private int port;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${internal.api-key}")
  private String internalApiKey;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private RestTestClient restClient;

  @BeforeEach
  void setUpClient() {
    restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void internalPathWithoutKeyReturns401() {
    String token = loginFreshUser("chain_one", "chain.one@example.com");

    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(PROBE_PATH)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("UNAUTHORIZED", errorCode(result));
  }

  @Test
  void internalPathWithWrongKeyReturns401() {
    String token = loginFreshUser("chain_two", "chain.two@example.com");

    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(PROBE_PATH)
            .header("Authorization", "Bearer " + token)
            .header("X-Internal-Key", "wrong-key")
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("UNAUTHORIZED", errorCode(result));
  }

  @Test
  void internalPathWithKeyAndValidTokenPassesBothGates() {
    String token = loginFreshUser("chain_three", "chain.three@example.com");

    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(PROBE_PATH)
            .header("Authorization", "Bearer " + token)
            .header("X-Internal-Key", internalApiKey)
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.NOT_FOUND, result.getStatus());
  }

  @Test
  void internalPathWithKeyAndExpiredTokenReturnsTokenExpired() {
    String expired =
        Jwts.builder()
            .subject("chain.four@example.com")
            .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
            .expiration(new Date(System.currentTimeMillis() - 3_600_000))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(PROBE_PATH)
            .header("Authorization", "Bearer " + expired)
            .header("X-Internal-Key", internalApiKey)
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("TOKEN_EXPIRED", errorCode(result));
  }

  private String loginFreshUser(String username, String email) {
    restClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "username",
                username,
                "email",
                email,
                "password",
                "StrongPass123!",
                "role",
                "MANAGER"))
        .exchange()
        .returnResult(String.class);

    EntityExchangeResult<String> login =
        restClient
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", email, "password", "StrongPass123!"))
            .exchange()
            .returnResult(String.class);

    try {
      return objectMapper.readTree(login.getResponseBody()).get("token").asText();
    } catch (Exception e) {
      throw new IllegalStateException("Login did not return a token", e);
    }
  }

  private String errorCode(EntityExchangeResult<String> result) {
    try {
      JsonNode node = objectMapper.readTree(result.getResponseBody());
      return node.get("error").asText();
    } catch (Exception e) {
      throw new IllegalStateException("Response is not the error contract", e);
    }
  }
}
