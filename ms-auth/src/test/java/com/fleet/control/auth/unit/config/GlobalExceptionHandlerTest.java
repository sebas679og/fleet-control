package com.fleet.control.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fleet.control.auth.common.exceptions.DuplicateEmailException;
import com.fleet.control.auth.common.exceptions.DuplicateUsernameException;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.common.exceptions.ErrorMessages;
import com.fleet.control.auth.common.exceptions.GlobalExceptionHandler;
import com.fleet.control.auth.common.exceptions.InvalidCredentialsException;
import com.fleet.control.auth.common.exceptions.TokenInvalidException;
import com.fleet.control.auth.models.dto.exceptions.response.ErrorResponse;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Unit tests proving the exceptions in action through the error contract. */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void invalidCredentialsMapsToUnauthorized() {
    ResponseEntity<ErrorResponse> response =
        handler.handleInvalidCredentials(
            new InvalidCredentialsException(ErrorMessages.INVALID_CREDENTIALS), null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(ErrorCodes.INVALID_CREDENTIALS, response.getBody().error());
  }

  @Test
  void duplicateEmailMapsToConflict() {
    ResponseEntity<ErrorResponse> response =
        handler.handleDuplicateEmail(
            new DuplicateEmailException(ErrorMessages.USER_ALREADY_EXISTS), null);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(ErrorCodes.USER_ALREADY_EXISTS, response.getBody().error());
  }

  @Test
  void duplicateUsernameMapsToConflict() {
    ResponseEntity<ErrorResponse> response =
        handler.handleDuplicateUsername(
            new DuplicateUsernameException(ErrorMessages.USERNAME_ALREADY_EXISTS), null);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(ErrorCodes.USER_ALREADY_EXISTS, response.getBody().error());
    assertEquals(ErrorMessages.USERNAME_ALREADY_EXISTS, response.getBody().message());
  }

  @Test
  void invalidTokenMapsToUnauthorizedPayload() {
    ResponseEntity<TokenPayload> response =
        handler.handleTokenInvalid(
            new TokenInvalidException(ErrorCodes.INVALID_TOKEN, ErrorMessages.INVALID_TOKEN));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(false, response.getBody().valid());
    assertEquals(ErrorCodes.INVALID_TOKEN, response.getBody().error());
  }

  @Test
  void validationFailureMapsToBadRequestWithDetails() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors())
        .thenReturn(List.of(new FieldError("registerRequest", "password", "Password is required")));

    ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(ErrorCodes.VALIDATION_ERROR, response.getBody().error());
    assertEquals(1, response.getBody().details().size());
    assertEquals("password", response.getBody().details().get(0).field());
  }
}
