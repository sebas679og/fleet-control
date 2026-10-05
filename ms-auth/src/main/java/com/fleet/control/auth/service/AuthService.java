package com.fleet.control.auth.service;

import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;

public interface AuthService {

  RegisterResponse createUser(RegisterRequest registerRequest);

  TokenResponse login(LoginRequest loginRequest);

  TokenPayload validateToken(String authHeader);
}
