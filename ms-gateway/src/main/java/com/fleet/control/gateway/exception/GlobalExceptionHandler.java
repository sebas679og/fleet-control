package com.fleet.control.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fleet.control.gateway.constants.GatewayErrorCodes;
import com.fleet.control.gateway.constants.GatewayMessages;
import com.fleet.control.gateway.dto.ErrorResponse;
import java.time.Instant;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    HttpStatus status;
    String error;
    String message;

    if (ex instanceof ResponseStatusException) {
      status = (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
      if (status == HttpStatus.GATEWAY_TIMEOUT) {
        status = HttpStatus.SERVICE_UNAVAILABLE;
        error = GatewayErrorCodes.SERVICE_UNAVAILABLE;
        message = GatewayMessages.SERVICE_UNAVAILABLE;
      } else {
        error = status.name();
        String reason = ((ResponseStatusException) ex).getReason();
        message = reason != null ? reason : status.getReasonPhrase();
      }
    } else {
      status = HttpStatus.SERVICE_UNAVAILABLE;
      error = GatewayErrorCodes.SERVICE_UNAVAILABLE;
      message = GatewayMessages.SERVICE_UNAVAILABLE;
    }

    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    ErrorResponse errorDetails =
        ErrorResponse.builder().error(error).message(message).timestamp(Instant.now()).build();

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(errorDetails);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
  }
}
