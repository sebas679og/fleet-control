package com.fleet.control.drivers.models.dtos.request;

import com.fleet.control.drivers.models.enums.DriverStatus;
import com.fleet.control.drivers.models.enums.LicenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Schema(description = "Full driver update data, including status and license expiry date.")
public record UpdateDriverRequest(
    @Schema(description = "Driver full name", example = "Jane Doe") @NotBlank @Size(max = 100)
        String fullName,
    @Schema(description = "Driver email address", example = "jane.doe@example.com")
        @NotBlank
        @Email
        @Size(max = 120)
        String email,
    @Schema(description = "Driver phone number", example = "+34 600 123 456")
        @NotBlank
        @Size(max = 30)
        String phone,
    @Schema(description = "Unique driving license number", example = "DL-2024-001234")
        @NotBlank
        @Size(max = 30)
        String licenseNumber,
    @Schema(description = "Driving license category", example = "C") @NotNull
        LicenseCategory licenseCategory,
    @Schema(
            description = "Driving license expiry date (must be in the future)",
            example = "2028-06-30")
        @NotNull
        @Future
        LocalDate licenseExpiresAt,
    @Schema(description = "Driver status", example = "ACTIVE") @NotNull DriverStatus status) {}
