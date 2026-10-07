package com.fleet.control.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Unit tests for the per-IP rate limit (US-05). */
class RateLimitFilterTest {

  private static final InetSocketAddress CLIENT = new InetSocketAddress("127.0.0.1", 12345);

  private MockServerWebExchange newExchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").remoteAddress(CLIENT));
  }

  private static GatewayFilterChain passingChain(AtomicBoolean called) {
    return exchange -> {
      called.set(true);
      return Mono.empty();
    };
  }

  @Test
  void requestsUnderLimitReachChain() {
    RateLimitFilter filter = new RateLimitFilter(2);
    AtomicBoolean called = new AtomicBoolean(false);

    filter.filter(newExchange(), passingChain(called)).block();

    assertTrue(called.get());
  }

  @Test
  void thirdRequestInWindowIsRejectedWith429() {
    RateLimitFilter filter = new RateLimitFilter(2);
    GatewayFilterChain chain = exchange -> Mono.empty();

    filter.filter(newExchange(), chain).block();
    filter.filter(newExchange(), chain).block();
    ServerWebExchange rejected = newExchange();
    filter.filter(rejected, chain).block();

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, rejected.getResponse().getStatusCode());
    assertNotNull(rejected.getResponse().getHeaders().getFirst("Retry-After"));
    assertEquals(MediaType.APPLICATION_JSON, rejected.getResponse().getHeaders().getContentType());
  }

  @Test
  void retryAfterIsPositiveNumberOfSeconds() {
    RateLimitFilter filter = new RateLimitFilter(1);
    GatewayFilterChain chain = exchange -> Mono.empty();

    filter.filter(newExchange(), chain).block();
    ServerWebExchange rejected = newExchange();
    filter.filter(rejected, chain).block();

    String retryAfter = rejected.getResponse().getHeaders().getFirst("Retry-After");
    assertNotNull(retryAfter);
    assertTrue(Long.parseLong(retryAfter) >= 1);
  }
}
