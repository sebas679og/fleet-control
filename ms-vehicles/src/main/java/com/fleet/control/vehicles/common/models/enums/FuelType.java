package com.fleet.control.vehicles.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** Fuel types supported by vehicles. */
@Schema(description = "Fuel types supported by vehicles")
public enum FuelType {
  DIESEL,
  GASOLINE,
  LPG,
  HYBRID
}
