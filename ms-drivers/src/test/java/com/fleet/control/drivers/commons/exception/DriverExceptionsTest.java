package com.fleet.control.drivers.commons.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import com.fleet.control.drivers.commons.exception.exception.DriverAlreadyExistsException;
import com.fleet.control.drivers.commons.exception.exception.DriverNotFoundException;
import com.fleet.control.drivers.commons.exception.exception.ValidationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DriverExceptionsTest {

  @Test
  void notFoundCarries404CodeAndId() {
    UUID id = UUID.randomUUID();

    DriverNotFoundException ex = new DriverNotFoundException(id);

    assertEquals(ErrorCode.DRIVER_NOT_FOUND, ex.getCode());
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    assertTrue(ex.getMessage().contains(id.toString()));
  }

  @Test
  void alreadyExistsCarries409CodeAndLicense() {
    String license = "DL-2024-001234";

    DriverAlreadyExistsException ex = new DriverAlreadyExistsException(license);

    assertEquals(ErrorCode.DRIVER_ALREADY_EXIST, ex.getCode());
    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    assertTrue(ex.getMessage().contains(license));
  }

  @Test
  void validationExposesFieldAndReason() {
    ValidationException ex = new ValidationException("size", "Page size must be between 1 and 100");

    assertEquals("size", ex.getField());
    assertEquals("Page size must be between 1 and 100", ex.getReason());
    assertEquals("Page size must be between 1 and 100", ex.getMessage());
  }
}
