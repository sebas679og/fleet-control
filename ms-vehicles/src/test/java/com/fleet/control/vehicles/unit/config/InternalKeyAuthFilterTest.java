package com.fleet.control.vehicles.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fleet.control.vehicles.config.InternalKeyAuthFilter;
import com.fleet.control.vehicles.config.SecurityConstants;
import com.fleet.control.vehicles.config.SecurityErrorHandler;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for the internal-key filter: valid key, wrong key and absent key. */
class InternalKeyAuthFilterTest {

  private static final String SHARED_KEY = "test-internal-key";

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private InternalKeyAuthFilter filter() {
    return new InternalKeyAuthFilter(new SecurityErrorHandler(), SHARED_KEY);
  }

  @Test
  void validKeyAuthenticatesAsManager() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(SecurityConstants.INTERNAL_KEY_HEADER, SHARED_KEY);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter().doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_MANAGER");
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void wrongKeyAnswers401WithoutContinuing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(SecurityConstants.INTERNAL_KEY_HEADER, "wrong-key");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter().doFilter(request, response, chain);

    verifyNoInteractions(chain);
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void absentKeyContinuesWithoutAuthentication() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter().doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
