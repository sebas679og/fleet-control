package com.fleet.control.gateway.config;

import java.util.List;

/** Route paths and public endpoints excluded from authentication. */
public final class ApiPathConstants {

  private ApiPathConstants() {}

  public static final String AUTH_REGISTER = "api/auth/register";
  public static final String AUTH_LOGIN = "api/auth/login";
  public static final String AUTH_VALIDATE = "api/auth/validate";

  public static final List<String> OPEN_API_ENDPOINTS =
      List.of(
          AUTH_REGISTER,
          AUTH_LOGIN,
          AUTH_VALIDATE,
          "/v3/api-docs",
          "/swagger-ui",
          "/health",
          "/actuator");
}
