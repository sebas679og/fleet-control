package com.fleet.control.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fleet.control.gateway.config.RouterValidator;
import com.fleet.control.gateway.dto.TokenPayload;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Unit tests for the (dormant) bearer validation filter. */
@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

  @Mock private WebClient.Builder webClientBuilder;
  @Mock private WebClient webClient;
  @Mock private WebClient.RequestBodyUriSpec bodyUriSpec;
  @Mock private WebClient.RequestHeadersSpec<?> headersSpec;
  @Mock private WebClient.ResponseSpec responseSpec;

  private AuthenticationFilter filter() {
    return new AuthenticationFilter(webClientBuilder, new RouterValidator());
  }

  private GatewayFilter apply() {
    return filter().apply(new AuthenticationFilter.Config());
  }

  @Test
  void openRouteSkipsValidation() {
    AtomicBoolean called = new AtomicBoolean(false);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login"));

    apply()
        .filter(
            exchange,
            ex -> {
              called.set(true);
              return Mono.empty();
            })
        .block();

    assertTrue(called.get());
  }

  @Test
  void securedRouteWithoutHeaderIsRejected() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/vehicles"));

    apply().filter(exchange, ex -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void securedRouteWithMalformedHeaderIsRejected() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HttpHeaders.AUTHORIZATION, "Basic x"));

    apply().filter(exchange, ex -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  private void mockValidation(Mono<TokenPayload> result) {
    when(webClientBuilder.build()).thenReturn(webClient);
    when(webClient.post()).thenReturn(bodyUriSpec);
    when(bodyUriSpec.uri(anyString())).thenReturn(bodyUriSpec);
    when(bodyUriSpec.header(anyString(), any())).thenReturn(bodyUriSpec);
    when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(TokenPayload.class)).thenReturn(result);
  }

  @Test
  void validTokenEnrichesRequest() {
    TokenPayload payload = TokenPayload.builder().valid(true).userId("u1").username("ana").build();
    mockValidation(Mono.just(payload));
    AtomicBoolean called = new AtomicBoolean(false);
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good"));

    apply()
        .filter(
            exchange,
            ex -> {
              called.set(true);
              return Mono.empty();
            })
        .block();

    assertTrue(called.get());
  }

  @Test
  void invalidTokenIsRejected() {
    mockValidation(Mono.just(TokenPayload.builder().valid(false).build()));
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad"));

    apply().filter(exchange, ex -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void authServiceErrorIsRejected() {
    mockValidation(Mono.error(new RuntimeException("down")));
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/vehicles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good"));

    apply().filter(exchange, ex -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }
}
