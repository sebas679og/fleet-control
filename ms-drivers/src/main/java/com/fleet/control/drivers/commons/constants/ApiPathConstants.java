package com.fleet.control.drivers.commons.constants;

public class ApiPathConstants {
  private ApiPathConstants() {}

  public static final String DRIVER_BASE_PATH = "/api/drivers";

  public static final String BY_ID = "/{driverId}";

  public static final String FULL_BY_ID = DRIVER_BASE_PATH + BY_ID; // /api/drivers/{driverId}
}
