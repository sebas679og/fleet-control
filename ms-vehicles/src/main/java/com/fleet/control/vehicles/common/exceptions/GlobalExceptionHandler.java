package com.fleet.control.vehicles.common.exceptions;

import com.fleet.control.vehicles.common.exceptions.dto.ErrorResponse;
import com.fleet.control.vehicles.common.exceptions.enums.ErrorCode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles vehicle API exceptions. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Domain errors (403, 404, 409...)
  /** Handles domain exceptions. */
  @ExceptionHandler(ApiVehicleException.class)
  public ResponseEntity<ErrorResponse> handleApiVehicleException(ApiVehicleException ex) {
    if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
      return ResponseEntity.status(ex.getStatus())
          .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }
    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
  }

  // 403 forbidden (MANAGER without permission)
  /** Handles access denied errors. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ErrorResponse.of(
                ErrorCode.FORBIDDEN, "You do not have permission to perform this action"));
  }

  // 400 with details
  /** Handles validation errors. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.Detail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> new ErrorResponse.Detail(f.getField(), f.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR, "Request contains invalid fields", details));
  }

  // 400 malformed type (invalid UUID, invalid enum, etc.)
  /** Handles unreadable messages. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Request contains invalid fields"));
  }

  // Fallback 500
  /** Handles unexpected errors. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"));
  }
}
