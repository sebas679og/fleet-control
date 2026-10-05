package com.fleet.control.auth.models.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "User registration data")
public record RegisterRequest(
    @Schema(description = "Unique username", example = "john_doe")
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^\\S+$", message = "Username cannot contain spaces")
        String username,
    @Schema(description = "Unique email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
    @Schema(
            description = "Password: min 8 characters, 1 uppercase letter, 1 number",
            example = "StrongPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter and one number")
        String password) {}
