package com.fleet.control.routes.commons.client.dto;

/** Payload to change a vehicle status in ms-vehicles (PATCH /api/vehicles/{id}/status). */
public record UpdateVehicleStatusRequest(String status, Integer odometerKm) {}
