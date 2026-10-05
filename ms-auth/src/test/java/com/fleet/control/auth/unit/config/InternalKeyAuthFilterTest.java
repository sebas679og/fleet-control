package com.fleet.control.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fleet.control.auth.common.constants.HeaderConstants;
import com.fleet.control.auth.config.InternalKeyAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

/** Unit tests for the internal service-to-service key filter. */
class InternalKeyAuthFilterTest {

  private final InternalKeyAuthFilter filter = new InternalKeyAuthFilter("secret-key");

  @Test
  void nonInternalPathPassesWithoutKey() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/api/auth/login");

    filter.doFilter(request, response, chain);

    then(chain).should().doFilter(request, response);
  }

  @Test
  void internalPathWithCorrectKeyPasses() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/internal/sync");
    when(request.getHeader(HeaderConstants.INTERNAL_KEY)).thenReturn("secret-key");

    filter.doFilter(request, response, chain);

    then(chain).should().doFilter(request, response);
  }

  @Test
  void internalPathWithWrongKeyThrows() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/internal/sync");
    when(request.getHeader(HeaderConstants.INTERNAL_KEY)).thenReturn("wrong-key");

    assertThrows(BadCredentialsException.class, () -> filter.doFilter(request, response, chain));
  }

  @Test
  void internalPathWithoutKeyThrows() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/internal/sync");
    when(request.getHeader(HeaderConstants.INTERNAL_KEY)).thenReturn(null);

    assertThrows(BadCredentialsException.class, () -> filter.doFilter(request, response, chain));
  }
}
