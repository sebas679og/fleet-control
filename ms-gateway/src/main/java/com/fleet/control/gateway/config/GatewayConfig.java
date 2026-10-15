package com.fleet.control.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Shared WebClient builder for gateway components. */
@Configuration
public class GatewayConfig {

  /** Shared WebClient builder used to call downstream services. */
  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }
}
