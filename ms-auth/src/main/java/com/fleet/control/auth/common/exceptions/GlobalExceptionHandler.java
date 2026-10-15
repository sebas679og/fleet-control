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

  /**
   * Handles domain errors keeping their status and code.
   *
   * @param ex the business failure
   * @return the error response with the exception status
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    return buildResponse(ex.getCode(), ex.getMessage(), ex.getStatus());
  }

  /**
   * Handles invalid tokens during explicit validation keeping the TokenPayload contract.
   *
   * @param ex the token failure
   * @return 401 with the TokenPayload contract
   */
  @ExceptionHandler(TokenInvalidException.class)
  public ResponseEntity<TokenPayload> handleTokenInvalid(TokenInvalidException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new TokenPayload(false, null, null, null, ex.getError(), ex.getMessage()));
  }

  /**
   * Handles expired JWTs keeping the TokenPayload contract.
   *
   * @param ex the expiration failure
   * @return 401 with the TokenPayload contract
   */
  @ExceptionHandler(ExpiredJwtException.class)
  public ResponseEntity<TokenPayload> handleExpiredJwt(ExpiredJwtException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            new TokenPayload(
                false, null, null, null, ErrorCode.TOKEN_EXPIRED.name(), "The token has expired"));
  }

  /**
   * Handles Spring Security authentication failures.
   *
   * @param ex the authentication failure
   * @return 401 with the error contract
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
    return buildResponse(
        ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles generic authentication failures.
   *
   * @param ex the authentication failure
   * @return 401 with the error contract
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
    return buildResponse(
        ErrorCode.UNAUTHORIZED, "Missing or invalid authentication token", HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles Spring Security access denials.
   *
   * @param ex the access denial
   * @return 403 with the error contract
   */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
      org.springframework.security.access.AccessDeniedException ex) {
    return buildResponse(
        ErrorCode.FORBIDDEN, "Access denied: insufficient permissions", HttpStatus.FORBIDDEN);
  }

  /**
   * Handles manual validation errors with field details.
   *
   * @param ex the validation failure
   * @return 400 with the field and reason
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    return buildDetailsResponse(
        ErrorCode.VALIDATION_ERROR,
        "Request contains invalid fields",
        List.of(new ErrorResponse.Violation(ex.getField(), ex.getReason())),
        HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles bean validation errors with field details.
   *
   * @param ex the binding failure
   * @return 400 with every field error
   */
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

  /**
   * Handles malformed bodies such as invalid UUID or enum values.
   *
   * @param ex the deserialization failure
   * @return 400 with the error contract
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return buildResponse(
        ErrorCode.VALIDATION_ERROR, "Request contains invalid fields", HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles a missing Authorization header.
   *
   * @param ex the missing header failure
   * @return 401 with the error contract
   */
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    return buildResponse(
        ErrorCode.UNAUTHORIZED, "Missing required header: Authorization", HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles unexpected failures, rethrowing unknown paths so Spring renders 404 instead of 500.
   *
   * @param ex the unexpected failure
   * @return 500 with the error contract
   * @throws Exception rethrown when the path does not exist
   */
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
