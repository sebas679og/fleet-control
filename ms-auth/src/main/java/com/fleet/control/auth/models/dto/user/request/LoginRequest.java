package com.fleet.control.auth.models.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "User login credentials")
public record LoginRequest(
    @Schema(description = "Registered email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
    @Schema(description = "User password", example = "StrongPass123!")
        @NotBlank(message = "Password is required")
        String password) {}
