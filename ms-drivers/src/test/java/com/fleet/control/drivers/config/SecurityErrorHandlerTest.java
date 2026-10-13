package com.fleet.control.drivers.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.control.drivers.commons.exception.enums.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

class SecurityErrorHandlerTest {

  private final SecurityErrorHandler handler = new SecurityErrorHandler();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void commenceWithoutMarkerReturnsUnauthorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.commence(request, response, new AuthenticationException("no auth") {});

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentType().contains("application/json"));
    Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
    assertEquals(ErrorCode.UNAUTHORIZED.name(), body.get("error"));
  }

  @Test
  void commenceWithExpiredMarkerReturnsTokenExpired() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(SecurityConstants.AUTH_ERROR_ATTRIBUTE, SecurityConstants.TOKEN_EXPIRED);
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.commence(request, response, new AuthenticationException("expired") {});

    assertEquals(401, response.getStatus());
    Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
    assertEquals(ErrorCode.TOKEN_EXPIRED.name(), body.get("error"));
  }

  @Test
  void handleReturnsForbidden() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("denied"));

    assertEquals(403, response.getStatus());
    Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
    assertEquals(ErrorCode.FORBIDDEN.name(), body.get("error"));
  }
}
