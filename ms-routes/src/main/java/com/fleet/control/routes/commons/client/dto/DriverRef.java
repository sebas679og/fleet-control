package com.fleet.control.routes.commons.client.dto;

import java.time.LocalDate;

/** Minimum driver data needed to validate a route plan. */
public record DriverRef(String status, String licenseCategory, LocalDate licenseExpiresAt) {}
