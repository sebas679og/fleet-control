package com.fleet.control.drivers.models.enums;

/** Driver availability: only {@code ACTIVE} drivers can be assigned to routes. */
public enum DriverStatus {
  ACTIVE,
  ON_LEAVE,
  SUSPENDED
}
