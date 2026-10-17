package com.fleet.control.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Role edge cases for route endpoints: MANAGER and ADMIN JWTs are allowed, any other role gets 403
 * FORBIDDEN without reaching the service, and anonymous callers get 401.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouteControllerRolesTest {

  private static final String BASE = "/api/routes";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @LocalServerPort private int port;

  @Value("${internal.api-key}")
  private String internalApiKey;

  @Value("${jwt.secret}")
  private String jwtSecret;

  private RestTestClient restClient;

  @BeforeEach
  void setUpClient() {
    restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  private static JsonNode body(EntityExchangeResult<String> result) {
    try {
      return MAPPER.readTree(result.getResponseBody());
    } catch (Exception e) {
      throw new IllegalStateException("Response is not JSON", e);
    }
  }

  private String signedJwt(String role, Instant expiresAt) {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claim("role", role)
        .expiration(Date.from(expiresAt))
        .signWith(key)
        .compact();
  }

  private EntityExchangeResult<String> getWithJwt(String path, String role) {
    return restClient
        .get()
        .uri(path)
        .headers(headers -> headers.setBearerAuth(signedJwt(role, Instant.now().plusSeconds(3600))))
        .exchange()
        .returnResult(String.class);
  }

  @Test
  void listWithManagerJwtIs200() {
    EntityExchangeResult<String> result = getWithJwt(BASE + "?page=0&size=20", "MANAGER");

    assertEquals(HttpStatus.OK, result.getStatus());
  }

  @Test
  void listWithAdminJwtIs200() {
    EntityExchangeResult<String> result = getWithJwt(BASE + "?page=0&size=20", "ADMIN");

    assertEquals(HttpStatus.OK, result.getStatus());
  }

  @Test
  void listWithOtherRoleJwtIsForbidden() {
    EntityExchangeResult<String> result = getWithJwt(BASE + "?page=0&size=20", "USER");

    assertEquals(HttpStatus.FORBIDDEN, result.getStatus());
    assertEquals("FORBIDDEN", body(result).get("error").asText());
  }

  @Test
  void listAnonymousIsUnauthorized() {
    EntityExchangeResult<String> result =
        restClient.get().uri(BASE).exchange().returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("UNAUTHORIZED", body(result).get("error").asText());
  }

  @Test
  void createWithOtherRoleJwtIsForbidden() {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", UUID.randomUUID().toString());
    body.put("driverId", UUID.randomUUID().toString());
    body.put("origin", "Madrid");
    body.put("destination", "Barcelona");
    body.put("plannedStart", Instant.now().plusSeconds(86400).toString());
    body.put("estimatedDurationMin", 240);
    body.put("plannedDistanceKm", "620.50");

    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(
                headers ->
                    headers.setBearerAuth(signedJwt("USER", Instant.now().plusSeconds(3600))))
            .body(body)
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.FORBIDDEN, result.getStatus());
    assertEquals("FORBIDDEN", body(result).get("error").asText());
  }
}
