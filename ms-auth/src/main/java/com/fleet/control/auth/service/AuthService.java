package com.fleet.control.auth.service;

import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;

/** Contract for user registration, login and token validation. */
public interface AuthService {

  /**
   * Registers a new user.
   *
   * @param registerRequest the registration input
   * @return the created user data
   */
  RegisterResponse createUser(RegisterRequest registerRequest);

  /**
   * Authenticates a user and issues a JWT.
   *
   * @param loginRequest the login credentials
   * @return the token response with user data
   */
  TokenResponse login(LoginRequest loginRequest);

  /**
   * Validates a bearer token.
   *
   * @param authHeader the {@code Authorization} header value
   * @return the token payload when the token is valid
   */
  TokenPayload validateToken(String authHeader);
}
