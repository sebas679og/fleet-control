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

@RestControllerAdvice
public class GlobalExceptionHandler {

  // Business errors (404, 409, 403...)
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
  }

  // ValidationException raised manually from services
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
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Request contains invalid fields"));
  }

  // Fallback
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"));
  }
}
