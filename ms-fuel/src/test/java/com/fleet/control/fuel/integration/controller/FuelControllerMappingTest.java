package com.fleet.control.fuel.integration.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.control.fuel.TestcontainersConfiguration;
import com.fleet.control.fuel.commons.client.VehiclesClient;
import com.fleet.control.fuel.commons.client.dto.VehicleRef;
import feign.FeignException;
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
import org.mockito.Mockito;
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
 * Error-mapping edge cases for refuel endpoints against a real PostgreSQL Testcontainer: downstream
 * failures and invalid input map to the documented status codes without leaking internals.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FuelControllerMappingTest {

  private static final String REFUELS = "/api/fuel/refuels";
  private static final String CONSUMPTION = "/api/fuel/consumption";
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

  private String managerJwt() {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claim("role", "MANAGER")
        .expiration(Date.from(Instant.now().plusSeconds(3600)))
        .signWith(key)
        .compact();
  }

  private EntityExchangeResult<String> postRefuel(Map<String, Object> payload) {
    return restClient
        .post()
        .uri(REFUELS)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(headers -> headers.setBearerAuth(managerJwt()))
        .body(payload)
        .exchange()
        .returnResult(String.class);
  }

  private Map<String, Object> refuelBody(UUID vehicleId, String liters, int odometer) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("vehicleId", vehicleId.toString());
    payload.put("refueledAt", Instant.now().toString());
    payload.put("liters", liters);
    payload.put("pricePerLiter", "1.62");
    payload.put("odometerKm", odometer);
    payload.put("fullTank", true);
    payload.put("station", "Repsol A-3 km 112");
    return payload;
  }

  private VehicleRef dieselVehicle(int odometer) {
    return new VehicleRef("2137KLM", "CAR", "DIESEL", 80, odometer, "AVAILABLE");
  }

  @Test
  void createUnknownVehicleIs404() {
    UUID vehicleId = UUID.randomUUID();
    given(vehiclesClient.getById(vehicleId)).willThrow(Mockito.mock(FeignException.NotFound.class));

    EntityExchangeResult<String> result = postRefuel(refuelBody(vehicleId, "38", 45500));

    assertEquals(HttpStatus.NOT_FOUND, result.getStatus());
    assertEquals("VEHICLE_NOT_FOUND", body(result).get("error").asText());
  }

  @Test
  void createOverCapacityIs400WithDetails() {
    UUID vehicleId = UUID.randomUUID();
    given(vehiclesClient.getById(vehicleId)).willReturn(dieselVehicle(45500));

    EntityExchangeResult<String> result = postRefuel(refuelBody(vehicleId, "90", 45500));

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
    assertEquals("VALIDATION_ERROR", body(result).get("error").asText());
    assertEquals("liters", body(result).get("details").get(0).get("field").asText());
  }

  @Test
  void createNegativeLitersIs400WithDetails() {
    UUID vehicleId = UUID.randomUUID();
    given(vehiclesClient.getById(vehicleId)).willReturn(dieselVehicle(45500));

    EntityExchangeResult<String> result = postRefuel(refuelBody(vehicleId, "-5", 45500));

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
    assertEquals("VALIDATION_ERROR", body(result).get("error").asText());
    assertEquals("liters", body(result).get("details").get(0).get("field").asText());
  }

  @Test
  void createOdometerRollbackIs409() {
    UUID vehicleId = UUID.randomUUID();
    given(vehiclesClient.getById(vehicleId)).willReturn(dieselVehicle(45500));

    EntityExchangeResult<String> first = postRefuel(refuelBody(vehicleId, "38", 45500));
    assertEquals(HttpStatus.CREATED, first.getStatus());

    EntityExchangeResult<String> rollback = postRefuel(refuelBody(vehicleId, "20", 45000));

    assertEquals(HttpStatus.CONFLICT, rollback.getStatus());
    assertEquals("INVALID_ODOMETER", body(rollback).get("error").asText());
  }

  @Test
  void createWhenVehiclesDownIs503() {
    UUID vehicleId = UUID.randomUUID();
    given(vehiclesClient.getById(any()))
        .willThrow(Mockito.mock(FeignException.ServiceUnavailable.class));

    EntityExchangeResult<String> result = postRefuel(refuelBody(vehicleId, "38", 45500));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatus());
    assertEquals("SERVICE_UNAVAILABLE", body(result).get("error").asText());
    assertEquals("vehicles", body(result).get("service").asText());
  }

  @Test
  void listInvalidPaginationIs400() {
    EntityExchangeResult<String> badPage =
        restClient
            .get()
            .uri(REFUELS + "?page=-1&size=20")
            .headers(headers -> headers.setBearerAuth(managerJwt()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.BAD_REQUEST, badPage.getStatus());
    assertEquals("VALIDATION_ERROR", body(badPage).get("error").asText());

    EntityExchangeResult<String> oversized =
        restClient
            .get()
            .uri(REFUELS + "?page=0&size=101")
            .headers(headers -> headers.setBearerAuth(managerJwt()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.BAD_REQUEST, oversized.getStatus());
    assertEquals("size", body(oversized).get("details").get(0).get("field").asText());
  }

  @Test
  void listFromAfterToIs400() {
    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(REFUELS + "?from=2026-10-03&to=2026-10-01")
            .headers(headers -> headers.setBearerAuth(managerJwt()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
    assertEquals("VALIDATION_ERROR", body(result).get("error").asText());
  }

  @Test
  void consumptionFromAfterToIs400() {
    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(CONSUMPTION + "?from=2026-10-03&to=2026-10-01")
            .headers(headers -> headers.setBearerAuth(managerJwt()))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
    assertEquals("VALIDATION_ERROR", body(result).get("error").asText());
  }
}
