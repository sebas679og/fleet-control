package com.fleet.control.auth.unit.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fleet.control.auth.common.constants.HeaderConstants;
import com.fleet.control.auth.config.InternalKeyAuthFilter;
import com.fleet.control.auth.config.SecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

/** Unit tests for the internal service-to-service key filter. */
class InternalKeyAuthFilterTest {

  private final SecurityErrorHandler errorHandler = mock(SecurityErrorHandler.class);
  private final InternalKeyAuthFilter filter =
      new InternalKeyAuthFilter(errorHandler, "secret-key");

  @Test
  void nonInternalPathPassesWithoutKey() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/api/auth/login");

    filter.doFilter(request, response, chain);

    then(chain).should().doFilter(request, response);
    then(errorHandler).should(never()).commence(any(), any(), any());
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
    then(errorHandler).should(never()).commence(any(), any(), any());
  }

  @Test
  void internalPathWithWrongKeyAnswers401WithoutContinuing() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/internal/sync");
    when(request.getHeader(HeaderConstants.INTERNAL_KEY)).thenReturn("wrong-key");

    filter.doFilter(request, response, chain);

    then(chain).should(never()).doFilter(request, response);
    then(errorHandler)
        .should()
        .commence(eq(request), eq(response), any(BadCredentialsException.class));
  }

  @Test
  void internalPathWithoutKeyAnswers401WithoutContinuing() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getServletPath()).thenReturn("/internal/sync");
    when(request.getHeader(HeaderConstants.INTERNAL_KEY)).thenReturn(null);

    filter.doFilter(request, response, chain);

    then(chain).should(never()).doFilter(request, response);
    then(errorHandler)
        .should()
        .commence(eq(request), eq(response), any(BadCredentialsException.class));
  }
}
