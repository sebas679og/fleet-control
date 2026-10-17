package com.fleet.control.fuel.commons.constants;

/** Fuel endpoint paths shared by the API contract. */
public class ApiPathConstants {

  private ApiPathConstants() {}

  public static final String FUEL_BASE_PATH = "/api/fuel";

  public static final String REFUELS = "/refuels";
  public static final String CONSUMPTION = "/consumption";
  public static final String STATS = "/stats";

  public static final String FULL_REFUELS = FUEL_BASE_PATH + REFUELS; // /api/fuel/refuels
  public static final String FULL_CONSUMPTION =
      FUEL_BASE_PATH + CONSUMPTION; // /api/fuel/consumption
  public static final String FULL_STATS = FUEL_BASE_PATH + STATS; // /api/fuel/stats
}
