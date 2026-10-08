package com.fleet.control.auth.common.exceptions;

import com.fleet.control.auth.models.dto.exceptions.response.ErrorResponse;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps controlled exceptions to the API error contract. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles invalid login credentials.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 401 response with {@code INVALID_CREDENTIALS}
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    return buildResponse(
        ErrorCodes.INVALID_CREDENTIALS, ErrorMessages.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles Spring Security authentication failures.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 401 response with {@code INVALID_CREDENTIALS}
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {
    return buildResponse(
        ErrorCodes.INVALID_CREDENTIALS, ErrorMessages.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles generic authentication failures.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 401 response with {@code UNAUTHORIZED}
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    return buildResponse(
        ErrorCodes.UNAUTHORIZED, ErrorMessages.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles access denials.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 403 response with {@code FORBIDDEN}
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return buildResponse(ErrorCodes.FORBIDDEN, ErrorMessages.FORBIDDEN, HttpStatus.FORBIDDEN);
  }

  /**
   * Handles Spring Security access denials.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 403 response with {@code FORBIDDEN}
   */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
      org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
    return buildResponse(ErrorCodes.FORBIDDEN, ErrorMessages.FORBIDDEN, HttpStatus.FORBIDDEN);
  }

  /**
   * Handles duplicate registration emails.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 409 response with {@code USER_ALREADY_EXISTS}
   */
  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateEmail(
      DuplicateEmailException ex, HttpServletRequest request) {
    return buildResponse(
        ErrorCodes.USER_ALREADY_EXISTS, ErrorMessages.USER_ALREADY_EXISTS, HttpStatus.CONFLICT);
  }

  /**
   * Handles duplicate registration usernames.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 409 response with {@code USER_ALREADY_EXISTS}
   */
  @ExceptionHandler(DuplicateUsernameException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateUsername(
      DuplicateUsernameException ex, HttpServletRequest request) {
    return buildResponse(
        ErrorCodes.USER_ALREADY_EXISTS, ErrorMessages.USERNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
  }

  /**
   * Handles invalid tokens during explicit validation.
   *
   * @param ex the exception raised
   * @return 401 response with the token payload marked as invalid
   */
  @ExceptionHandler(TokenInvalidException.class)
  public ResponseEntity<TokenPayload> handleTokenInvalid(TokenInvalidException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new TokenPayload(false, null, null, null, ex.getError(), ex.getMessage()));
  }

  /**
   * Handles expired JWTs.
   *
   * @param ex the exception raised
   * @return 401 response with the token payload marked as expired
   */
  @ExceptionHandler(ExpiredJwtException.class)
  public ResponseEntity<TokenPayload> handleExpiredJwt(ExpiredJwtException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            new TokenPayload(
                false, null, null, null, ErrorCodes.TOKEN_EXPIRED, ErrorMessages.TOKEN_EXPIRED));
  }

  /**
   * Handles missing {@code Authorization} headers.
   *
   * @param ex the exception raised
   * @return 401 response with {@code UNAUTHORIZED}
   */
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    return buildResponse(
        ErrorCodes.UNAUTHORIZED,
        ErrorMessages.MISSING_AUTHORIZATION_HEADER,
        HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handles bean validation failures with field-level details.
   *
   * @param ex the exception raised
   * @param request the current request
   * @return 400 response with {@code VALIDATION_ERROR} and details
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ErrorResponse.Violation> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorResponse.Violation(error.getField(), error.getDefaultMessage()))
            .toList();
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .error(ErrorCodes.VALIDATION_ERROR)
            .message(ErrorMessages.VALIDATION_FAILED)
            .timestamp(Instant.now())
            .details(details)
            .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      String error, String message, HttpStatus status) {
    ErrorResponse errorResponse =
        ErrorResponse.builder().error(error).message(message).timestamp(Instant.now()).build();
    return ResponseEntity.status(status).body(errorResponse);
  }
}
