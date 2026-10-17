package com.fleet.control.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
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
 * Role edge cases for vehicle endpoints: MANAGER and ADMIN JWTs are allowed, any other role gets
 * 403 FORBIDDEN without reaching the service, and anonymous callers get 401.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VehicleControllerRolesTest {

  private static final String BASE = "/api/vehicles";
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

  private Map<String, Object> createBody(String plate) {
    Map<String, Object> body = new HashMap<>();
    body.put("plate", plate);
    body.put("make", "Ford");
    body.put("model", "Transit");
    body.put("year", LocalDate.now().getYear());
    body.put("type", "VAN");
    body.put("fuelType", "DIESEL");
    body.put("tankCapacityL", 80);
    body.put("odometerKm", 45210);
    return body;
  }

  private String uniquePlate() {
    return "TST-" + UUID.randomUUID().toString().substring(0, 8);
  }

  @Test
  void listWithManagerJwtIs200() {
    EntityExchangeResult<String> result = getWithJwt(BASE, "MANAGER");

    assertEquals(HttpStatus.OK, result.getStatus());
  }

  @Test
  void listWithAdminJwtIs200() {
    EntityExchangeResult<String> result = getWithJwt(BASE, "ADMIN");

    assertEquals(HttpStatus.OK, result.getStatus());
  }

  @Test
  void listWithOtherRoleJwtIsForbidden() {
    EntityExchangeResult<String> result = getWithJwt(BASE, "USER");

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
  void createWithAdminJwtIs201() {
    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(
                headers ->
                    headers.setBearerAuth(signedJwt("ADMIN", Instant.now().plusSeconds(3600))))
            .body(createBody(uniquePlate()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.CREATED, result.getStatus());
    assertTrue(body(result).hasNonNull("id"));
    assertEquals("AVAILABLE", body(result).get("status").asText());
  }

  @Test
  void createWithOtherRoleJwtIsForbidden() {
    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(
                headers ->
                    headers.setBearerAuth(signedJwt("USER", Instant.now().plusSeconds(3600))))
            .body(createBody(uniquePlate()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.FORBIDDEN, result.getStatus());
    assertEquals("FORBIDDEN", body(result).get("error").asText());
  }
}
