package com.fleet.control.auth.models.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Data returned after a successful user registration. */
@Schema(description = "User registration response")
public record RegisterResponse(
    @Schema(description = "Created user ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,
    @Schema(description = "Username", example = "john_doe") String username,
    @Schema(description = "Email address", example = "john.doe@example.com") String email,
    @Schema(description = "User role", example = "MANAGER") String role,
    @Schema(description = "Creation timestamp", example = "2025-01-15T10:30:00Z")
        Instant createdAt) {}
