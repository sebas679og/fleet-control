package com.fleet.control.auth.service.impl;

import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import com.fleet.control.auth.models.mappers.UserMapper;
import com.fleet.control.auth.repository.UserEntityRepository;
import com.fleet.control.auth.service.AuthService;
import com.fleet.control.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

  private final UserEntityRepository userEntityRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UserMapper userMapper;

  @Override
  public RegisterResponse createUser(RegisterRequest registerRequest) {
    return null;
  }

  @Override
  public TokenResponse login(LoginRequest loginRequest) {
    return null;
  }

  @Override
  public TokenPayload validateToken(String authHeader) {
    return null;
  }
}
