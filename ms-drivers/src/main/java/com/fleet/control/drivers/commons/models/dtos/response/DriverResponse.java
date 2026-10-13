package com.fleet.control.drivers.commons.models.dtos.response;

import com.fleet.control.drivers.commons.models.enums.DriverStatus;
import com.fleet.control.drivers.commons.models.enums.LicenseCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String licenseNumber,
        LicenseCategory licenseCategory,
        LocalDate licenseExpiresAt,
        DriverStatus status,
        Instant createdAt,
        Instant updatedAt) {

}