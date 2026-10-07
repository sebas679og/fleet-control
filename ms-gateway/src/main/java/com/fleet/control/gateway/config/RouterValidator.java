package com.fleet.control.gateway.config;

import java.util.function.Predicate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/** Decides whether a route requires authentication. */
@Component
public class RouterValidator {

  private final Predicate<ServerHttpRequest> securedRoutePredicate =
      request ->
          ApiPathConstants.OPEN_API_ENDPOINTS.stream()
              .noneMatch(uri -> request.getURI().getPath().contains(uri));

  public boolean isSecured(ServerHttpRequest request) {
    return securedRoutePredicate.test(request);
  }
}
