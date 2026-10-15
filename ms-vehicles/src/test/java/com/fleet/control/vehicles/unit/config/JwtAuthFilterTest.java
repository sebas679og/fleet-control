package com.fleet.control.vehicles.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fleet.control.vehicles.config.JwtAuthFilter;
import com.fleet.control.vehicles.config.SecurityConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for the JWT filter: valid token, expired token and garbage. */
class JwtAuthFilterTest {

  private static final String SECRET = "test_secret_of_at_least_32_chars_123456";

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private String token(String role, long lifetimeMs) {
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject("user-id")
        .claim(SecurityConstants.CLAIM_ROLE, role)
        .issuedAt(new Date(now))
        .expiration(new Date(now + lifetimeMs))
        .signWith(key)
        .compact();
  }

  @Test
  void shortSecretIsRejected() {
    assertThatThrownBy(() -> new JwtAuthFilter("too-short"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void validTokenAuthenticatesWithRole() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN", 60000));
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    new JwtAuthFilter(SECRET).doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void expiredTokenMarksTokenExpired() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token("MANAGER", -1000));
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    new JwtAuthFilter(SECRET).doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(request.getAttribute(SecurityConstants.AUTH_ERROR_ATTRIBUTE))
        .isEqualTo(SecurityConstants.TOKEN_EXPIRED);
  }

  @Test
  void garbageTokenLeavesNoMarker() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    new JwtAuthFilter(SECRET).doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(request.getAttribute(SecurityConstants.AUTH_ERROR_ATTRIBUTE)).isNull();
  }

  @Test
  void missingHeaderContinuesWithoutAuthentication() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    new JwtAuthFilter(SECRET).doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
