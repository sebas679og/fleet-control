package com.fleet.control.auth.models.dto.token.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/** User information extracted from a validated JWT. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User information extracted from JWT token")
public record TokenPayload(
    @Schema(description = "Whether the token is valid", example = "true") boolean valid,
    @Schema(description = "Unique user ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String userId,
    @Schema(description = "Username", example = "john_doe") String username,
    @Schema(description = "User role", example = "MANAGER") String role,
    @Schema(
            description = "Error code if the token is invalid",
            example = "TOKEN_EXPIRED",
            nullable = true)
        String error,
    @Schema(
            description = "Error description if the token is invalid",
            example = "The token has expired",
            nullable = true)
        String message) {}
