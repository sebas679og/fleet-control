package com.fleet.control.fuel.integration.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.control.fuel.TestcontainersConfiguration;
import com.fleet.control.fuel.commons.client.VehiclesClient;
import com.fleet.control.fuel.commons.client.dto.VehicleRef;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Role edge cases for refuel endpoints: MANAGER and ADMIN JWTs are allowed, any other role gets 403
 * FORBIDDEN and anonymous callers get 401.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FuelControllerRolesTest {

  private static final String BASE = "/api/fuel/refuels";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @LocalServerPort private int port;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @MockitoBean private VehiclesClient vehiclesClient;

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

  private Map<String, Object> validCreateBody(UUID vehicleId) {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("refueledAt", Instant.now().toString());
    body.put("liters", "38");
    body.put("pricePerLiter", "1.62");
    body.put("odometerKm", 45500);
    body.put("fullTank", true);
    body.put("station", "Repsol A-3 km 112");
    return body;
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
  void createWithManagerJwtIs201() {
    UUID vehicleId = UUID.randomUUID();
    given(vehiclesClient.getById(vehicleId))
        .willReturn(new VehicleRef("2137KLM", "CAR", "DIESEL", 80, 45500, "AVAILABLE"));

    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(
                headers ->
                    headers.setBearerAuth(signedJwt("MANAGER", Instant.now().plusSeconds(3600))))
            .body(validCreateBody(vehicleId))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.CREATED, result.getStatus());
    assertNotNull(body(result).get("id").asText());
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
            .body(validCreateBody(UUID.randomUUID()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.FORBIDDEN, result.getStatus());
    assertEquals("FORBIDDEN", body(result).get("error").asText());
  }
}
