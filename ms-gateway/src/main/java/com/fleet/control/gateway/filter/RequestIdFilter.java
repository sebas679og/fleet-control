package com.fleet.control.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Adds X-Request-Id when the external request does not carry one. */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

  public static final String X_REQUEST_ID = "X-Request-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String requestId = exchange.getRequest().getHeaders().getFirst(X_REQUEST_ID);
    ServerWebExchange filtered = exchange;
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
      ServerHttpRequest mutated =
          exchange.getRequest().mutate().header(X_REQUEST_ID, requestId).build();
      filtered = exchange.mutate().request(mutated).build();
    }
    filtered.getResponse().getHeaders().set(X_REQUEST_ID, requestId);
    return chain.filter(filtered);
  }

  @Override
  public int getOrder() {
    return -2;
  }
}
