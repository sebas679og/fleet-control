package com.fleet.control.gateway.constants;

/** Header names propagated by the gateway. */
public final class GatewayHeaders {

  private GatewayHeaders() {}

  public static final String X_USER_ID = "X-User-Id";
  public static final String X_USER_ROLE = "X-User-Role";
  public static final String X_USER_NAME = "X-User-Name";
  public static final String BEARER_PREFIX = "Bearer ";
}
