package com.fleet.control.drivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** End-to-end driver flow against a real PostgreSQL Testcontainer, including security gates. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DriverSecurityIntegrationTest {

  private static final String BASE = "/api/drivers";
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

  private String signedJwt(Instant expiresAt) {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claim("role", "MANAGER")
        .expiration(Date.from(expiresAt))
        .signWith(key)
        .compact();
  }

  private Map<String, Object> createBody(String license) {
    Map<String, Object> body = new HashMap<>();
    body.put("fullName", "Jane Doe");
    body.put("email", "jane.doe." + UUID.randomUUID() + "@example.com");
    body.put("phone", "+34 600 123 456");
    body.put("licenseNumber", license);
    body.put("licenseCategory", "C");
    body.put("licenseExpiresAt", "2030-06-30");
    return body;
  }

  private EntityExchangeResult<String> postDriver(Map<String, Object> body, String internalKey) {
    return restClient
        .post()
        .uri(BASE)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Internal-Key", internalKey)
        .body(body)
        .exchange()
        .returnResult(String.class);
  }

  private EntityExchangeResult<String> getWithInternalKey(String path) {
    return restClient
        .get()
        .uri(path)
        .header("X-Internal-Key", internalApiKey)
        .exchange()
        .returnResult(String.class);
  }

  @Test
  void listWithoutCredentialsIs401() {
    EntityExchangeResult<String> result =
        restClient.get().uri(BASE).exchange().returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("UNAUTHORIZED", body(result).get("error").asText());
  }

  @Test
  void createWithoutCredentialsIs401() {
    EntityExchangeResult<String> result =
        restClient
            .post()
            .uri(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createBody("DL-SEC-1"))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
  }

  @Test
  void listWithWrongInternalKeyIs401() {
    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(BASE)
            .header("X-Internal-Key", "wrong-key")
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("UNAUTHORIZED", body(result).get("error").asText());
  }

  @Test
  void happyPathWithInternalKey() {
    String license = "DL-HAPPY-" + UUID.randomUUID().toString().substring(0, 8);

    // Create -> 201
    EntityExchangeResult<String> created = postDriver(createBody(license), internalApiKey);
    assertEquals(HttpStatus.CREATED, created.getStatus());
    assertTrue(body(created).hasNonNull("id"));
    assertEquals("ACTIVE", body(created).get("status").asText());
    String id = body(created).get("id").asText();

    // Get by id -> 200
    EntityExchangeResult<String> found = getWithInternalKey(BASE + "/" + id);
    assertEquals(HttpStatus.OK, found.getStatus());
    assertEquals(license, body(found).get("licenseNumber").asText());
    assertEquals("C", body(found).get("licenseCategory").asText());

    // List -> 200 paginated
    EntityExchangeResult<String> listed = getWithInternalKey(BASE);
    assertEquals(HttpStatus.OK, listed.getStatus());
    assertTrue(body(listed).get("totalElements").asLong() >= 1);

    // Update -> 200
    Map<String, Object> update = createBody(license);
    update.put("phone", "+34 600 999 999");
    update.put("status", "SUSPENDED");
    EntityExchangeResult<String> updated =
        restClient
            .put()
            .uri(BASE + "/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Internal-Key", internalApiKey)
            .body(update)
            .exchange()
            .returnResult(String.class);
    assertEquals(HttpStatus.OK, updated.getStatus());
    assertEquals("+34 600 999 999", body(updated).get("phone").asText());
    assertEquals("SUSPENDED", body(updated).get("status").asText());

    // Duplicate license -> 409
    EntityExchangeResult<String> duplicate = postDriver(createBody(license), internalApiKey);
    assertEquals(HttpStatus.CONFLICT, duplicate.getStatus());
    assertEquals("DRIVER_ALREADY_EXISTS", body(duplicate).get("error").asText());

    // Unknown id -> 404
    EntityExchangeResult<String> missing = getWithInternalKey(BASE + "/" + UUID.randomUUID());
    assertEquals(HttpStatus.NOT_FOUND, missing.getStatus());
    assertEquals("DRIVER_NOT_FOUND", body(missing).get("error").asText());

    // Invalid pagination -> 400
    EntityExchangeResult<String> badPage = getWithInternalKey(BASE + "?page=-1");
    assertEquals(HttpStatus.BAD_REQUEST, badPage.getStatus());
    assertEquals("VALIDATION_ERROR", body(badPage).get("error").asText());

    // Invalid body -> 400 with details
    Map<String, Object> invalid = createBody("DL-BAD-" + UUID.randomUUID());
    invalid.put("email", "not-an-email");
    EntityExchangeResult<String> invalidResponse = postDriver(invalid, internalApiKey);
    assertEquals(HttpStatus.BAD_REQUEST, invalidResponse.getStatus());
    assertEquals("VALIDATION_ERROR", body(invalidResponse).get("error").asText());
    assertTrue(body(invalidResponse).hasNonNull("details"));
  }

  @Test
  void listWithValidJwtIs200() {
    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(BASE)
            .headers(headers -> headers.setBearerAuth(signedJwt(Instant.now().plusSeconds(3600))))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.OK, result.getStatus());
  }

  @Test
  void listWithExpiredJwtIs401() {
    EntityExchangeResult<String> result =
        restClient
            .get()
            .uri(BASE)
            .headers(headers -> headers.setBearerAuth(signedJwt(Instant.now().minusSeconds(60))))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatus());
    assertEquals("TOKEN_EXPIRED", body(result).get("error").asText());
  }
}
