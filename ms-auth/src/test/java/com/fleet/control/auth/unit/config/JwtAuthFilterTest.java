package com.fleet.control.auth.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.config.JwtAuthFilter;
import com.fleet.control.auth.config.SecurityErrorHandler;
import com.fleet.control.auth.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Unit tests for the JWT request filter authentication paths. */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtAuthFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthFilter(jwtService, userDetailsService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterWithoutAuthHeaderPassesThrough() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(null);

    filter.doFilter(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterWithNonBearerHeaderPassesThrough() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Basic some-token");

    filter.doFilter(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterWithInvalidJwtPassesThrough() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer bad-token");
    given(jwtService.extractEmail("bad-token")).willThrow(new RuntimeException("Invalid JWT"));

    filter.doFilter(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterWithExpiredJwtPassesThrough() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer expired-token");
    given(jwtService.extractEmail("expired-token"))
        .willThrow(
            new IllegalArgumentException(
                "Invalid or expired JWT", new ExpiredJwtException(null, null, "JWT expired")));

    filter.doFilter(request, response, filterChain);

    then(filterChain).should().doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterWithValidJwtSetsAuthentication() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer valid-token");
    given(jwtService.extractEmail("valid-token")).willReturn("user@example.com");
    var userDetails = new User("user@example.com", "pass", List.of());
    given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(userDetails);

    filter.doFilter(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isEqualTo(userDetails);
    assertThat(auth.getCredentials()).isNull();
    then(filterChain).should().doFilter(request, response);
  }

  @Test
  void doFilterWithExistingAuthenticationDoesNotOverride() throws Exception {
    var existingAuth = new UsernamePasswordAuthenticationToken("existing", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
    then(filterChain).should().doFilter(request, response);
  }

  @Test
  void doFilterWhenUserNotFoundPropagatesException() {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer valid-token");
    given(jwtService.extractEmail("valid-token")).willReturn("unknown@example.com");
    given(userDetailsService.loadUserByUsername("unknown@example.com"))
        .willThrow(new UsernameNotFoundException("User not found"));

    assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  void doFilterWithInvalidJwtMarksUnauthorized() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer bad-token");
    given(jwtService.extractEmail("bad-token")).willThrow(new RuntimeException("Invalid JWT"));

    filter.doFilter(request, response, filterChain);

    then(request)
        .should()
        .setAttribute(SecurityErrorHandler.AUTH_ERROR_ATTRIBUTE, ErrorCodes.UNAUTHORIZED);
  }

  @Test
  void doFilterWithExpiredJwtMarksTokenExpired() throws Exception {
    given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn("Bearer expired-token");
    given(jwtService.extractEmail("expired-token"))
        .willThrow(
            new IllegalArgumentException(
                "Invalid or expired JWT", new ExpiredJwtException(null, null, "JWT expired")));

    filter.doFilter(request, response, filterChain);

    then(request)
        .should()
        .setAttribute(SecurityErrorHandler.AUTH_ERROR_ATTRIBUTE, ErrorCodes.TOKEN_EXPIRED);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
