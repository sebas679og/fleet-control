package com.fleet.control.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fleet.control.auth.config.AuthDemoSeeder;
import com.fleet.control.auth.models.entities.UserEntity;
import com.fleet.control.auth.models.enums.UserRole;
import com.fleet.control.auth.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the demo admin seeder. */
@ExtendWith(MockitoExtension.class)
class AuthDemoSeederTest {

  @Mock private UserEntityRepository userEntityRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private AuthDemoSeeder seeder;

  @BeforeEach
  void setUp() {
    seeder = new AuthDemoSeeder(userEntityRepository, passwordEncoder);
    ReflectionTestUtils.setField(seeder, "adminPassword", "Seeded_Admin123");
  }

  @Test
  void createsAdminWhenAbsent() {
    given(
            userEntityRepository.existsByEmailOrUsername(
                AuthDemoSeeder.ADMIN_EMAIL, AuthDemoSeeder.ADMIN_USERNAME))
        .willReturn(false);
    given(passwordEncoder.encode("Seeded_Admin123")).willReturn("encoded-password");

    seeder.run(null);

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userEntityRepository).save(captor.capture());
    assertEquals(AuthDemoSeeder.ADMIN_EMAIL, captor.getValue().getEmail());
    assertEquals(AuthDemoSeeder.ADMIN_USERNAME, captor.getValue().getUsername());
    assertEquals("encoded-password", captor.getValue().getPassword());
    assertEquals(UserRole.ADMIN, captor.getValue().getRole());
  }

  @Test
  void skipsSeedWhenAdminExists() {
    given(
            userEntityRepository.existsByEmailOrUsername(
                AuthDemoSeeder.ADMIN_EMAIL, AuthDemoSeeder.ADMIN_USERNAME))
        .willReturn(true);

    seeder.run(null);

    verify(userEntityRepository, never()).save(any(UserEntity.class));
  }
}
