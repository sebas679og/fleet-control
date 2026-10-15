package com.fleet.control.auth.repository;

import com.fleet.control.auth.models.entities.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence access for platform users. */
@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, UUID> {

  /**
   * Finds a user by email.
   *
   * @param email the unique email address
   * @return the user when present
   */
  Optional<UserEntity> findByEmail(String email);

  /**
   * Finds a user by username.
   *
   * @param username the unique username
   * @return the user when present
   */
  Optional<UserEntity> findByUsername(String username);

  /**
   * Checks whether an email is already registered.
   *
   * @param email the email to check
   * @return true when a user exists with that email
   */
  boolean existsByEmail(String email);

  /**
   * Checks whether a username is already registered.
   *
   * @param username the username to check
   * @return true when a user exists with that username
   */
  boolean existsByUsername(String username);

  /**
   * Checks whether an email or a username is already registered, in a single query.
   *
   * @param email the email to check
   * @param username the username to check
   * @return true when a user exists with either value
   */
  boolean existsByEmailOrUsername(String email, String username);
}
