package com.fleet.control.auth.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.control.auth.common.path.ApiPaths;
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

/** End-to-end authentication flow against a real PostgreSQL Testcontainer. */
class AuthApiControllerIntegrationTest extends AbstractIntegrationTest {

  @LocalServerPort private int port;

  @Value("${jwt.secret}")
  private String jwtSecret;

  private RestTestClient restClient;

  @BeforeEach
  void setUpClient() {
    restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void registerReturns201WithManagerRole() {
    EntityExchangeResult<String> result =
        postJson(ApiPaths.REGISTER_ENDPOINT, registerBody("it_one", "it.one@example.com"));

    assertEquals(HttpStatus.CREATED, result.getStatus());
    assertEquals("it_one", body(result).get("username").asText());
    assertEquals("MANAGER", body(result).get("role").asText());
  }

  @Test
  void registerDuplicateReturns409() {
    Map<String, String> body = registerBody("it_two", "it.two@example.com");
    postJson(ApiPaths.REGISTER_ENDPOINT, body);

    EntityExchangeResult<String> result = postJson(ApiPaths.REGISTER_ENDPOINT, body);

    assertEquals(HttpStatus.CONFLICT, result.getStatus());
    assertEquals("USER_ALREADY_EXISTS", body(result).get("error").asText());
  }

  @Test
  void loginReturns200WithToken() {
    postJson(ApiPaths.REGISTER_ENDPOINT, registerBody("it_three", "it.three@example.com"));

    EntityExchangeResult<String> result =
        postJson(
            ApiPaths.LOGIN_ENDPOINT,
            Map.of("email", "it.three@example.com", "password", "StrongPass123!"));

    assertEquals(HttpStatus.OK, result.getStatus());
    assertTrue(body(result).hasNonNull("token"));
    assertEquals("Bearer", body(result).get("tokenType").asText());
    assertEquals("MANAGER", body(result).get("role").asText());
  }

  @Test
  void loginWithWrongPasswordReturns401() {
    postJson(ApiPaths.REGISTER_ENDPOINT, registerBody("it_four", "it.four@example.com"));

    EntityExchangeResult<String> result =
        postJson(
            ApiPaths.LOGIN_ENDPOINT,
            Map.of("email", "it.four@example.com", "password", "WrongPass123!"));

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("INVALID_CREDENTIALS", body(result).get("error").asText());
  }

  @Test
  void validateValidTokenReturns200() {
    postJson(ApiPaths.REGISTER_ENDPOINT, registerBody("it_five", "it.five@example.com"));
    String token = login("it.five@example.com", "StrongPass123!");

    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(ApiPaths.VALIDATE_ENDPOINT)
            .headers(headers -> headers.setBearerAuth(token))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.OK, result.getStatus());
    assertTrue(body(result).get("valid").asBoolean());
    assertEquals("it_five", body(result).get("username").asText());
  }

  @Test
  void validateInvalidTokenReturns401() {
    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(ApiPaths.VALIDATE_ENDPOINT)
            .headers(headers -> headers.setBearerAuth("bad-token"))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertFalse(body(result).get("valid").asBoolean());
  }

  @Test
  void protectedPathWithoutTokenReturnsUnauthorizedContract() {
    EntityExchangeResult<String> result =
        restClient.get().uri("/api/auth/users").exchange().returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("UNAUTHORIZED", body(result).get("error").asText());
  }

  @Test
  void protectedPathWithExpiredTokenReturnsTokenExpired() {
    String expiredToken = expiredToken("it-expired@example.com");

    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri("/api/auth/users")
            .headers(headers -> headers.setBearerAuth(expiredToken))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("TOKEN_EXPIRED", body(result).get("error").asText());
  }

  private EntityExchangeResult<String> postJson(String path, Map<String, String> body) {
    return restClient
        .post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange()
        .returnResult(String.class);
  }

  private String login(String email, String password) {
    return body(postJson(ApiPaths.LOGIN_ENDPOINT, Map.of("email", email, "password", password)))
        .get("token")
        .asText();
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode body(EntityExchangeResult<String> result) {
    try {
      return MAPPER.readTree(result.getResponseBody());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Response is not JSON", e);
    }
  }

  private static Map<String, String> registerBody(String username, String email) {
    return Map.of("username", username, "email", email, "password", "StrongPass123!");
  }

  private String expiredToken(String email) {
    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date(System.currentTimeMillis() - 7200000L))
        .expiration(new Date(System.currentTimeMillis() - 3600000L))
        .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }
}
