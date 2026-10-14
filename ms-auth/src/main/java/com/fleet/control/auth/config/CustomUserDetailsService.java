package com.fleet.control.auth.config;

import com.fleet.control.auth.repository.UserEntityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users by email (login) or id (JWT subject) for Spring Security authentication. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserEntityRepository userEntityRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userEntityRepository
        .findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User Not Found" + email));
  }

  /**
   * Loads a user by id, used when the JWT subject carries the user identifier.
   *
   * @param id the user identifier
   * @return the user details
   * @throws UsernameNotFoundException when no user exists with the id
   */
  public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
    return userEntityRepository
        .findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User Not Found" + id));
  }
}
