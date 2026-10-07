package com.fleet.control.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.config.SecurityErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

/** Unit tests for the security chain error contract. */
class SecurityErrorHandlerTest {

  private final SecurityErrorHandler handler = new SecurityErrorHandler();

  @Test
  void commenceWithoutMarkerReturnsUnauthorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.commence(request, response, new BadCredentialsException("bad"));

    assertEquals(401, response.getStatus());
    JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
    assertEquals(ErrorCodes.UNAUTHORIZED, body.get("error").asText());
  }

  @Test
  void commenceWithExpiredMarkerReturnsTokenExpired() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(SecurityErrorHandler.AUTH_ERROR_ATTRIBUTE, ErrorCodes.TOKEN_EXPIRED);
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.commence(request, response, new BadCredentialsException("bad"));

    assertEquals(401, response.getStatus());
    JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
    assertEquals(ErrorCodes.TOKEN_EXPIRED, body.get("error").asText());
  }

  @Test
  void handleReturnsForbidden() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("denied"));

    assertEquals(403, response.getStatus());
    JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
    assertEquals(ErrorCodes.FORBIDDEN, body.get("error").asText());
  }
}
