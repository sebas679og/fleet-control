package com.fleet.control.drivers.commons.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.fleet.control.drivers.commons.exception.dto.ErrorResponse;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import com.fleet.control.drivers.commons.exception.exception.DriverAlreadyExistsException;
import com.fleet.control.drivers.commons.exception.exception.DriverNotFoundException;
import com.fleet.control.drivers.commons.exception.exception.ValidationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleApiMapsNotFound() {
    UUID id = UUID.randomUUID();

    ResponseEntity<ErrorResponse> response = handler.handleApi(new DriverNotFoundException(id));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCode.DRIVER_NOT_FOUND.name(), response.getBody().error());
    assertNull(response.getBody().details());
  }

  @Test
  void handleApiMapsConflict() {
    ResponseEntity<ErrorResponse> response =
        handler.handleApi(new DriverAlreadyExistsException("DL-1"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCode.DRIVER_ALREADY_EXIST.name(), response.getBody().error());
  }

  @Test
  void handleValidationMapsFieldDetails() {
    ResponseEntity<ErrorResponse> response =
        handler.handleValidation(
            new ValidationException("page", "Page index must be zero or greater"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().error());
    assertNotNull(response.getBody().details());
    assertEquals(1, response.getBody().details().size());
    assertEquals("page", response.getBody().details().get(0).field());
    assertEquals(
        "Page index must be zero or greater", response.getBody().details().get(0).reason());
  }

  @Test
  void handleUnreadableMapsBadRequest() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnreadable(mock(HttpMessageNotReadableException.class));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().error());
  }

  @Test
  void handleGenericMapsInternalError() {
    ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCode.INTERNAL_ERROR.name(), response.getBody().error());
  }
}
