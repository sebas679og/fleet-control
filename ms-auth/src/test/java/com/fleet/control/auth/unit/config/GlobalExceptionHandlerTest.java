package com.fleet.control.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fleet.control.auth.common.exceptions.ApiException;
import com.fleet.control.auth.common.exceptions.GlobalExceptionHandler;
import com.fleet.control.auth.common.exceptions.dto.ErrorResponse;
import com.fleet.control.auth.common.exceptions.enums.ErrorCode;
import com.fleet.control.auth.common.exceptions.exception.AccessDeniedException;
import com.fleet.control.auth.common.exceptions.exception.DuplicateEmailException;
import com.fleet.control.auth.common.exceptions.exception.DuplicateUsernameException;
import com.fleet.control.auth.common.exceptions.exception.InvalidCredentialsException;
import com.fleet.control.auth.common.exceptions.exception.TokenInvalidException;
import com.fleet.control.auth.common.exceptions.exception.ValidationException;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Unit tests proving the exceptions in action through the error contract. */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void invalidCredentialsMapsToUnauthorized() {
    ResponseEntity<ErrorResponse> response = handler.handleApi(new InvalidCredentialsException());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(ErrorCode.INVALID_CREDENTIALS.name(), response.getBody().error());
  }

  @Test
  void duplicateEmailMapsToConflict() {
    ResponseEntity<ErrorResponse> response = handler.handleApi(new DuplicateEmailException());

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(ErrorCode.USER_ALREADY_EXISTS.name(), response.getBody().error());
  }

  @Test
  void duplicateUsernameMapsToConflict() {
    ResponseEntity<ErrorResponse> response = handler.handleApi(new DuplicateUsernameException());

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(ErrorCode.USER_ALREADY_EXISTS.name(), response.getBody().error());
    assertEquals("A user with that username already exists", response.getBody().message());
  }

  @Test
  void accessDeniedMapsToForbidden() {
    ResponseEntity<ErrorResponse> response = handler.handleApi(new AccessDeniedException());

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals(ErrorCode.FORBIDDEN.name(), response.getBody().error());
  }

  @Test
  void apiExceptionCarriesCodeAndStatus() {
    ApiException ex = new DuplicateEmailException("custom");

    assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getCode());
    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    assertEquals("custom", ex.getMessage());
  }

  @Test
  void invalidTokenMapsToUnauthorizedPayload() {
    ResponseEntity<TokenPayload> response =
        handler.handleTokenInvalid(
            new TokenInvalidException(ErrorCode.INVALID_TOKEN, "The token is invalid"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(false, response.getBody().valid());
    assertEquals(ErrorCode.INVALID_TOKEN.name(), response.getBody().error());
  }

  @Test
  void manualValidationMapsToBadRequestWithDetails() {
    ResponseEntity<ErrorResponse> response =
        handler.handleValidation(new ValidationException("password", "Password is required"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().error());
    assertEquals(1, response.getBody().details().size());
    assertEquals("password", response.getBody().details().get(0).field());
  }

  @Test
  void validationFailureMapsToBadRequestWithDetails() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors())
        .thenReturn(List.of(new FieldError("registerRequest", "password", "Password is required")));

    ResponseEntity<ErrorResponse> response = handler.handleBeanValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().error());
    assertEquals(1, response.getBody().details().size());
    assertEquals("password", response.getBody().details().get(0).field());
  }

  @Test
  void unreadableMessageMapsToBadRequest() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnreadable(mock(HttpMessageNotReadableException.class));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().error());
  }

  @Test
  void genericFailureMapsToInternalError() throws Exception {
    ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(ErrorCode.INTERNAL_ERROR.name(), response.getBody().error());
  }
}
