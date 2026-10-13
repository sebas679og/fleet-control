package com.fleet.control.auth.common.exceptions;

import com.fleet.control.auth.common.exceptions.dto.ErrorResponse;
import com.fleet.control.auth.common.exceptions.enums.ErrorCode;
import com.fleet.control.auth.common.exceptions.exception.TokenInvalidException;
import com.fleet.control.auth.common.exceptions.exception.ValidationException;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps controlled exceptions to the API error contract (ms-drivers architecture).
 *
 * <p>Canonical handlers ({@code handleApi}, {@code handleValidation}, {@code handleBeanValidation},
 * {@code handleUnreadable}, {@code handleGeneric}) mirror ms-drivers. The {@code TokenPayload}
 * handlers are an intentional auth-only exception: the {@code validateToken} endpoint answers with
 * the same payload shape for valid and invalid tokens.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Domain errors (401, 403, 409...) — also covers the custom AccessDeniedException.
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    return buildResponse(ex.getCode(), ex.getMessage(), ex.getStatus());
  }

  // Invalid tokens during explicit validation keep the TokenPayload contract (401).
  @ExceptionHandler(TokenInvalidException.class)
  public ResponseEntity<TokenPayload> handleTokenInvalid(TokenInvalidException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new TokenPayload(false, null, null, null, ex.getError(), ex.getMessage()));
  }

  // Expired JWTs keep the TokenPayload contract (401).
  @ExceptionHandler(ExpiredJwtException.class)
  public ResponseEntity<TokenPayload> handleExpiredJwt(ExpiredJwtException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            new TokenPayload(
                false, null, null, null, ErrorCode.TOKEN_EXPIRED.name(), "The token has expired"));
  }

  // Spring Security authentication failures (401).
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
    return buildResponse(
        ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", HttpStatus.UNAUTHORIZED);
  }

  // Generic authentication failures (401).
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
    return buildResponse(
        ErrorCode.UNAUTHORIZED, "Missing or invalid authentication token", HttpStatus.UNAUTHORIZED);
  }

  // Spring Security access denials (403).
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
      org.springframework.security.access.AccessDeniedException ex) {
    return buildResponse(
        ErrorCode.FORBIDDEN, "Access denied: insufficient permissions", HttpStatus.FORBIDDEN);
  }

  // ValidationException raised manually from services (400 with details).
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    return buildDetailsResponse(
        ErrorCode.VALIDATION_ERROR,
        "Request contains invalid fields",
        List.of(new ErrorResponse.Violation(ex.getField(), ex.getReason())),
        HttpStatus.BAD_REQUEST);
  }

  // @Valid on DTOs (400 with details).
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.Violation> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorResponse.Violation(error.getField(), error.getDefaultMessage()))
            .toList();
    return buildDetailsResponse(
        ErrorCode.VALIDATION_ERROR,
        "Request contains invalid fields",
        details,
        HttpStatus.BAD_REQUEST);
  }

  // Malformed type (invalid UUID or enum value) (400).
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return buildResponse(
        ErrorCode.VALIDATION_ERROR, "Request contains invalid fields", HttpStatus.BAD_REQUEST);
  }

  // Missing Authorization header (401).
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    return buildResponse(
        ErrorCode.UNAUTHORIZED, "Missing required header: Authorization", HttpStatus.UNAUTHORIZED);
  }

  // Fallback (500). Unknown paths are rethrown so Spring renders 404 instead of 500.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) throws Exception {
    if (ex instanceof NoResourceFoundException) {
      throw ex;
    }
    return buildResponse(
        ErrorCode.INTERNAL_ERROR, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      ErrorCode error, String message, HttpStatus status) {
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .error(error.name())
            .message(message)
            .timestamp(Instant.now())
            .build();
    return ResponseEntity.status(status).body(errorResponse);
  }

  private ResponseEntity<ErrorResponse> buildDetailsResponse(
      ErrorCode error, String message, List<ErrorResponse.Violation> details, HttpStatus status) {
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .error(error.name())
            .message(message)
            .timestamp(Instant.now())
            .details(details)
            .build();
    return ResponseEntity.status(status).body(errorResponse);
  }
}
