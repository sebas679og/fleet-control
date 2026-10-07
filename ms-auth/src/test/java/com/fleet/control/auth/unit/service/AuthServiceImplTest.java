package com.fleet.control.auth.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleet.control.auth.common.exceptions.DuplicateEmailException;
import com.fleet.control.auth.common.exceptions.InvalidCredentialsException;
import com.fleet.control.auth.models.dto.user.request.LoginRequest;
import com.fleet.control.auth.models.dto.user.request.RegisterRequest;
import com.fleet.control.auth.models.dto.user.response.RegisterResponse;
import com.fleet.control.auth.models.entities.UserEntity;
import com.fleet.control.auth.models.enums.UserRole;
import com.fleet.control.auth.models.mappers.UserMapper;
import com.fleet.control.auth.repository.UserEntityRepository;
import com.fleet.control.auth.service.JwtService;
import com.fleet.control.auth.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for the registration happy path and its main edge cases. */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private UserEntityRepository userEntityRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserMapper userMapper;

  @InjectMocks private AuthServiceImpl authService;

  @Test
  void createUserRegistersManagerWithEncodedPassword() {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("john_doe")
            .email("john.doe@example.com")
            .password("StrongPass123!")
            .build();
    UserEntity mapped =
        UserEntity.builder()
            .username("john_doe")
            .email("john.doe@example.com")
            .role(UserRole.MANAGER)
            .build();
    UserEntity saved =
        UserEntity.builder()
            .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
            .username("john_doe")
            .email("john.doe@example.com")
            .password("encoded")
            .role(UserRole.MANAGER)
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

    when(userEntityRepository.existsByEmailOrUsername(request.email(), request.username()))
        .thenReturn(false);
    when(userMapper.toUserEntity(request)).thenReturn(mapped);
    when(passwordEncoder.encode(request.password())).thenReturn("encoded");
    when(userEntityRepository.save(any(UserEntity.class))).thenReturn(saved);

    RegisterResponse response = authService.createUser(request);

    assertEquals("550e8400-e29b-41d4-a716-446655440000", response.id());
    assertEquals("john_doe", response.username());
    assertEquals("john.doe@example.com", response.email());
    assertEquals("MANAGER", response.role());
    assertEquals("encoded", mapped.getPassword());
    verify(userEntityRepository).save(mapped);
  }

  @Test
  void createUserRejectsDuplicateEmailWithoutSaving() {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("john_doe")
            .email("john.doe@example.com")
            .password("StrongPass123!")
            .build();

    when(userEntityRepository.existsByEmailOrUsername(request.email(), request.username()))
        .thenReturn(true);

    assertThrows(DuplicateEmailException.class, () -> authService.createUser(request));
    verify(userEntityRepository, never()).save(any(UserEntity.class));
  }

  @Test
  void loginRejectsInvalidCredentials() {
    LoginRequest request =
        LoginRequest.builder().email("john.doe@example.com").password("WrongPass123!").build();

    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
  }
}
