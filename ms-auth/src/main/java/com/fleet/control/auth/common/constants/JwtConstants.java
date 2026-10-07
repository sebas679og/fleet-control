package com.fleet.control.auth.common.constants;

/** Shared JWT claim names, prefixes and token settings. */
public class JwtConstants {

  private JwtConstants() {}

  public static final String TOKEN_TYPE_BEARER = "Bearer";
  public static final String BEARER_PREFIX = TOKEN_TYPE_BEARER + " ";
  public static final String ROLE_PREFIX = "ROLE_";

  public static final String CLAIM_USER_ID = "userId";
  public static final String CLAIM_USERNAME = "username";
  public static final String CLAIM_ROLE = "role";

  public static final long EXPIRES_IN_SECONDS_DEFAULT = 3600L;
}
