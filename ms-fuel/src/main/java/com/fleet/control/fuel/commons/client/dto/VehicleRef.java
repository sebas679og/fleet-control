package com.fleet.control.fuel.commons.client.dto;

/** Minimum vehicle data needed to validate refuels and enrich responses. */
public record VehicleRef(
    String plate,
    String type,
    String fuelType,
    Integer tankCapacityL,
    Integer odometerKm,
    String status) {}
