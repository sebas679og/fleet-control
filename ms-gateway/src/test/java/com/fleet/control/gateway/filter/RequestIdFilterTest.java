package com.fleet.control.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Unit tests for the X-Request-Id handling (US-04). */
class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void missingHeaderGeneratesUuidAndReflectsIt() {
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain =
        exchange -> {
          forwarded.set(exchange);
          return Mono.empty();
        };
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/test"));

    filter.filter(exchange, chain).block();

    String responseId = exchange.getResponse().getHeaders().getFirst(RequestIdFilter.X_REQUEST_ID);
    assertNotNull(responseId);
    UUID.fromString(responseId);
    assertEquals(
        responseId,
        forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.X_REQUEST_ID));
  }

  @Test
  void existingHeaderIsRespected() {
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain =
        exchange -> {
          forwarded.set(exchange);
          return Mono.empty();
        };
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test")
                .header(RequestIdFilter.X_REQUEST_ID, "prueba-123"));

    filter.filter(exchange, chain).block();

    assertEquals(
        "prueba-123", exchange.getResponse().getHeaders().getFirst(RequestIdFilter.X_REQUEST_ID));
    assertEquals(
        "prueba-123",
        forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.X_REQUEST_ID));
  }
}
