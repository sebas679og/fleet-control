package com.fleet.control.drivers.models.dtos.response;

import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.models.enums.LicenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Driver data returned by the API. */
@Schema(description = "Driver data returned by the API")
public record DriverResponse(
    @Schema(
            description = "Driver unique identifier",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
    @Schema(description = "Driver full name", example = "Jane Doe") String fullName,
    @Schema(description = "Driver email address", example = "jane.doe@example.com") String email,
    @Schema(description = "Driver phone number", example = "+34 600 123 456") String phone,
    @Schema(description = "Unique driving license number", example = "DL-2024-001234")
        String licenseNumber,
    @Schema(description = "Driving license category", example = "C")
        LicenseCategory licenseCategory,
    @Schema(description = "Driving license expiry date", example = "2028-06-30")
        LocalDate licenseExpiresAt,
    @Schema(description = "Driver status", example = "ACTIVE") DriverStatus status,
    @Schema(description = "Creation timestamp", example = "2025-01-15T10:30:00Z") Instant createdAt,
    @Schema(description = "Last update timestamp", example = "2025-02-20T14:45:00Z")
        Instant updatedAt) {}
