package com.fleet.control.auth.config;

import com.fleet.control.auth.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users by email for Spring Security authentication. */
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
}
