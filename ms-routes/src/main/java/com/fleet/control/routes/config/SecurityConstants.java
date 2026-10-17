package com.fleet.control.routes.config;

/** Shared security constants: JWT claims, roles and internal headers. */
public final class SecurityConstants {

  private SecurityConstants() {}

  public static final String BEARER_PREFIX = "Bearer ";
  public static final String ROLE_PREFIX = "ROLE_";
  public static final String CLAIM_ROLE = "role";

  public static final String ROLE_ADMIN = "ADMIN";
  public static final String ROLE_MANAGER = "MANAGER";

  public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

  public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";
  public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
}
