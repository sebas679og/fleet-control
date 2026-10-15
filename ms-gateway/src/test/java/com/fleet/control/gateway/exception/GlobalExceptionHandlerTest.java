package com.fleet.control.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for the 503 mapping of gateway errors. */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void gatewayTimeoutBecomesServiceUnavailable() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/vehicles"));

    handler.handle(exchange, new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT)).block();

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
  }

  @Test
  void otherStatusesKeepTheirCode() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/vehicles"));

    handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

    assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
  }

  @Test
  void unexpectedErrorsBecomeServiceUnavailable() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/vehicles"));

    handler.handle(exchange, new RuntimeException("boom")).block();

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
  }
}
