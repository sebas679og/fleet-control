package com.fleet.control.routes.commons.constants;

/** Route endpoint paths shared by the API contract. */
public class ApiPathConstants {
  private ApiPathConstants() {}

  public static final String ROUTE_BASE_PATH = "/api/routes";

  public static final String BY_ID = "/{routeId}";
  public static final String START = "/{routeId}/start";
  public static final String COMPLETE = "/{routeId}/complete";
  public static final String STATS = "/stats";

  public static final String FULL_BY_ID = ROUTE_BASE_PATH + BY_ID; // /api/routes/{routeId}
  public static final String FULL_START = ROUTE_BASE_PATH + START; // /api/routes/{routeId}/start
  public static final String FULL_COMPLETE =
      ROUTE_BASE_PATH + COMPLETE; // /api/routes/{routeId}/complete
  public static final String FULL_STATS = ROUTE_BASE_PATH + STATS; // /api/routes/stats
}
