package com.fleet.control.auth.controller;

import com.fleet.control.auth.common.path.ApiPaths;
import com.fleet.control.auth.models.dto.exceptions.response.ErrorResponse;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/** Authentication endpoints: registration, login and token validation. */
@RequestMapping(ApiPaths.AUTH_BASE)
@Tag(name = "Auth", description = "Authentication and user management operations")
public interface AuthApi {

  @Operation(
      summary = "User registration",
      description =
          "Creates a new user in the system. Validates the input, "
              + "verifies that the email is not already registered, "
              + "encrypts the password with BCrypt, "
              + "persists it to the database and returns the created user data.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or password does not meet the requirements",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "The email address is already registered",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPaths.REGISTER)
  ResponseEntity<RegisterResponse> createUser(@Valid @RequestBody RegisterRequest registerRequest);

  @Operation(
      summary = "Sign in",
      description =
          "Authenticates an existing user with email and password. "
              + "Spring Security verifies the credentials against the database "
              + "and, when valid, issues a JWT for subsequent requests.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sign-in successful",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid or missing input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials (incorrect email or password)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping(ApiPaths.LOGIN)
  ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest);

  @Operation(
      summary = "Validate JWT",
      description =
          "Verifies whether a JWT is valid and not expired.\n\n"
              + "How to use this endpoint:\n"
              + "1. Obtain a token via POST /api/auth/login\n"
              + "2. Click the Authorize button (top right) and paste the token as: Bearer <token>\n"
              + "3. The Authorization header will then be sent automatically with this endpoint")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Valid token — returns the user data (userId, username, role)",
            content = @Content(schema = @Schema(implementation = TokenPayload.class))),
        @ApiResponse(
            responseCode = "401",
            description =
                "Invalid token, expired, or with an incorrect signature — "
                    + "returns the error (TOKEN_EXPIRED / INVALID_TOKEN)",
            content = @Content(schema = @Schema(implementation = TokenPayload.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing Authorization header (required)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  @PostMapping(ApiPaths.VALIDATE)
  ResponseEntity<TokenPayload> validateToken(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader);
}
