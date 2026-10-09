package com.fleet.control.vehicles.common.constants;

public class ApiPathConstants {

    private ApiPathConstants() {}

    public static final String VEHICLE_BASE_PATH = "/api/vehicles";

    public static final String BY_ID    = "/{vehicleId}";
    public static final String STATUS   = "/{vehicleId}/status";
    public static final String SUMMARY  = "/summary";

    public static final String FULL_BY_ID   = VEHICLE_BASE_PATH + BY_ID;    // /api/vehicles/{vehicleId}
    public static final String FULL_STATUS  = VEHICLE_BASE_PATH + STATUS;   // /api/vehicles/{vehicleId}/status
    public static final String FULL_SUMMARY = VEHICLE_BASE_PATH + SUMMARY;  // /api/vehicles/summary
}
