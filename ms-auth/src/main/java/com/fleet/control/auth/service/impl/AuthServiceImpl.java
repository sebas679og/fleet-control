package com.fleet.control.auth.service.impl;

import com.fleet.control.auth.common.constants.JwtConstants;
import com.fleet.control.auth.common.exceptions.DuplicateEmailException;
import com.fleet.control.auth.common.exceptions.DuplicateUsernameException;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.common.exceptions.ErrorMessages;
import com.fleet.control.auth.common.exceptions.InvalidCredentialsException;
import com.fleet.control.auth.common.exceptions.TokenInvalidException;
import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import com.fleet.control.auth.models.entities.UserEntity;
import com.fleet.control.auth.models.mappers.UserMapper;
import com.fleet.control.auth.repository.UserEntityRepository;
import com.fleet.control.auth.service.AuthService;
import com.fleet.control.auth.service.JwtService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  @Transactional
  public RegisterResponse createUser(RegisterRequest registerRequest) {
    if (userEntityRepository.existsByEmail(registerRequest.email())) {
      throw new DuplicateEmailException(ErrorMessages.USER_ALREADY_EXISTS);
    }
    if (userEntityRepository.existsByUsername(registerRequest.username())) {
      throw new DuplicateUsernameException(ErrorMessages.USERNAME_ALREADY_EXISTS);
    }

    UserEntity user = userMapper.toUserEntity(registerRequest);
    user.setPassword(passwordEncoder.encode(registerRequest.password()));
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());

    UserEntity saved = userEntityRepository.save(user);
    log.info("User created: {}", saved.getEmail());

    return new RegisterResponse(
        saved.getId().toString(),
        saved.getUsername(),
        saved.getEmail(),
        saved.getRole().name(),
        saved.getCreatedAt());
  }

  @Override
  public TokenResponse login(LoginRequest loginRequest) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.email(), loginRequest.password()));

      UserEntity user = (UserEntity) authentication.getPrincipal();

      log.info("User logged in: {}", user.getEmail());

      return jwtService.generateToken(
          user.getEmail(), user.getId().toString(), user.getUsername(), user.getRole().name());
    } catch (BadCredentialsException e) {
      log.warn("Failed login attempt for email: {}", loginRequest.email());
      throw new InvalidCredentialsException(ErrorMessages.INVALID_CREDENTIALS);
    }
  }

  @Override
  public TokenPayload validateToken(String authHeader) {
    if (authHeader == null || authHeader.isBlank()) {
      throw new TokenInvalidException(ErrorCodes.INVALID_TOKEN, ErrorMessages.UNAUTHORIZED);
    }
    String token = authHeader.replace(JwtConstants.BEARER_PREFIX, "").trim();
    TokenPayload payload = jwtService.validateToken(token);
    if (!payload.valid()) {
      throw new TokenInvalidException(payload.error(), payload.message());
    }
    return payload;
  }
}
