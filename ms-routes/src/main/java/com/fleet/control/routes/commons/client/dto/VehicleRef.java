package com.fleet.control.routes.commons.client.dto;

/** Minimum vehicle data needed to validate and execute route plans. */
public record VehicleRef(String status, String type, Integer odometerKm) {}
