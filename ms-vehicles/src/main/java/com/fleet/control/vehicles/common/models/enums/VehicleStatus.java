package com.fleet.control.vehicles.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** Lifecycle statuses of a vehicle. */
@Schema(description = "Lifecycle statuses of a vehicle")
public enum VehicleStatus {
  AVAILABLE,
  IN_USE,
  IN_MAINTENANCE,
  OUT_OF_SERVICE
}
