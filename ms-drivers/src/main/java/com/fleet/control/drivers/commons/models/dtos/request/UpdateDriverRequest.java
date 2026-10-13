package com.fleet.control.drivers.commons.models.dtos.request;

import com.fleet.control.drivers.commons.models.enums.DriverStatus;
import com.fleet.control.drivers.commons.models.enums.LicenseCategory;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UpdateDriverRequest(
        @NotBlank
        @Size(max = 100)
        String fullName,
        @NotBlank
        @Email
        @Size(max = 120)
        String email,
        @NotBlank
        @Size(max = 30)
        String phone,
        @NotBlank
        @Size(max = 30)
        String licenseNumber,
        @NotNull
        LicenseCategory licenseCategory,
        @NotNull
        @Future
        LocalDate licenseExpiresAt,
        @NotNull
        DriverStatus status)
 {
}
