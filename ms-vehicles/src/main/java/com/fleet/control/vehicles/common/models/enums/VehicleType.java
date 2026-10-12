package com.fleet.control.vehicles.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** Types of vehicles managed by the fleet. */
@Schema(description = "Types of vehicles managed by the fleet")
public enum VehicleType {
  CAR,
  VAN,
  TRUCK,
  MOTORCYCLE
}
