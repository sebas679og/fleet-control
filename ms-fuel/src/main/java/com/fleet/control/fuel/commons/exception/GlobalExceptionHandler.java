package com.fleet.control.fuel.commons.exception;

import com.fleet.control.fuel.commons.exception.dto.ErrorResponse;
import com.fleet.control.fuel.commons.exception.enums.ErrorCode;
import com.fleet.control.fuel.commons.exception.exception.ServiceUnavailableException;
import com.fleet.control.fuel.commons.exception.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps service exceptions to the common error contract. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String VALIDATION_ERROR_MESSAGE = "Request contains invalid fields";

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.ofService(ex.getService()));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
  }

  /** Maps field validation failures to 400 with details. */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                VALIDATION_ERROR_MESSAGE,
                List.of(new ErrorResponse.Detail(ex.getField(), ex.getReason()))));
  }

  /** Maps bean validation failures to 400 with details. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.Detail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> new ErrorResponse.Detail(f.getField(), f.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, VALIDATION_ERROR_MESSAGE, details));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, VALIDATION_ERROR_MESSAGE));
  }

  /** Maps method-security denials to 403. */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ErrorResponse.of(
                ErrorCode.FORBIDDEN, "You do not have permission to perform this action"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"));
  }
}
