package com.fleet.control.auth.models.dto.token.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/** JWT issued on successful login. */
@Builder
@Schema(description = "JWT authentication token")
public record TokenResponse(
    @Schema(
            description = "JWT to use in Authorization header",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,
    @Schema(description = "Token type", example = "Bearer") String tokenType,
    @Schema(description = "Token lifetime in seconds", example = "3600") long expiresIn,
    @Schema(description = "Authenticated user ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String userId,
    @Schema(description = "User role", example = "MANAGER") String role) {}
