package com.fleet.control.routes.commons.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** Adds the service-to-service internal key to downstream calls. */
public class FeignAuthConfig {

  /** Creates the interceptor with the configured internal key. */
  @Bean
  public RequestInterceptor internalKeyInterceptor(
      @Value("${internal.api-key}") String internalApiKey) {
    return template -> template.header("X-Internal-Key", internalApiKey);
  }
}
