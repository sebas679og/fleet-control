package com.fleet.control.auth.controller.impl;

import com.fleet.control.auth.controller.AuthApi;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthApiController implements AuthApi {

  @Override
  public ResponseEntity<RegisterResponse> createUser(RegisterRequest registerRequest) {
    return null;
  }

  @Override
  public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
    return null;
  }

  @Override
  public ResponseEntity<TokenPayload> validateToken(String authHeader) {
    return null;
  }
}
