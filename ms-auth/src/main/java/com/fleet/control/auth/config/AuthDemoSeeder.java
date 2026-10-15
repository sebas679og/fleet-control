package com.fleet.control.auth.config;

import com.fleet.control.auth.models.entities.UserEntity;
import com.fleet.control.auth.models.enums.UserRole;
import com.fleet.control.auth.repository.UserEntityRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Demo data generator: creates the {@code ADMIN} user when the users table is empty. Only runs with
 * the {@code demo} profile.
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class AuthDemoSeeder implements ApplicationRunner {

  public static final String ADMIN_EMAIL = "admin@fleetcontrol.com";
  public static final String ADMIN_USERNAME = "admin";

  private final UserEntityRepository userEntityRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${demo.admin-password:Change_Admin123}")
  private String adminPassword;

  @Override
  public void run(ApplicationArguments args) {
    if (userEntityRepository.existsByEmailOrUsername(ADMIN_EMAIL, ADMIN_USERNAME)) {
      log.info("Demo admin user already exists, skipping seed");
      return;
    }
    UserEntity admin =
        UserEntity.builder()
            .username(ADMIN_USERNAME)
            .email(ADMIN_EMAIL)
            .password(passwordEncoder.encode(adminPassword))
            .role(UserRole.ADMIN)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    userEntityRepository.save(admin);
    log.info("Demo admin user created");
  }
}
