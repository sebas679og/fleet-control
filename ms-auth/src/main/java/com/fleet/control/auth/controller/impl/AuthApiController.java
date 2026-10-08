package com.fleet.control.auth.controller.impl;

import com.fleet.control.auth.common.constants.HeaderConstants;
import com.fleet.control.auth.controller.AuthApi;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import com.fleet.control.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** REST implementation of the authentication endpoints. */
@RestController
@RequiredArgsConstructor
public class AuthApiController implements AuthApi {

  private final AuthService authService;

  @Override
  public ResponseEntity<RegisterResponse> createUser(
      @RequestBody @Valid RegisterRequest registerRequest) {
    RegisterResponse response = authService.createUser(registerRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
    TokenResponse response = authService.login(loginRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<TokenPayload> validateToken(
      @RequestHeader(HeaderConstants.AUTHORIZATION) String authHeader) {
    return ResponseEntity.ok(authService.validateToken(authHeader));
  }
}
