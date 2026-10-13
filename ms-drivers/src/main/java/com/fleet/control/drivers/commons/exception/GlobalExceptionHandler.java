package com.fleet.control.drivers.commons.exception;

import com.fleet.control.drivers.commons.exception.dto.ErrorResponse;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import com.fleet.control.drivers.commons.exception.exception.ValidationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps controlled and unexpected failures to the common error contract. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Business errors (404, 409, 403...)
  /**
   * Handles controlled business errors keeping their status and code.
   *
   * @param ex the business failure
   * @return the error response with the exception status
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
  }

  // ValidationException raised manually from services
  /**
   * Handles manual validation errors with field details.
   *
   * @param ex the validation failure
   * @return 400 with the field and reason
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                "Request contains invalid fields",
                List.of(new ErrorResponse.Detail(ex.getField(), ex.getReason()))));
  }

  // @Valid on DTOs (400 with details)
  /**
   * Handles bean validation errors with field details.
   *
   * @param ex the binding failure
   * @return 400 with every field error
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.Detail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> new ErrorResponse.Detail(f.getField(), f.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR, "Request contains invalid fields", details));
  }

  // 400 for malformed type (invalid UUID or enum)
  /**
   * Handles unreadable bodies (invalid UUID or enum values).
   *
   * @param ex the parsing failure
   * @return 400 without details
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Request contains invalid fields"));
  }

  // Fallback
  /**
   * Handles unexpected failures hiding internal details.
   *
   * @param ex the unexpected failure
   * @return 500 with a generic code
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"));
  }
}
