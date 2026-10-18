package com.fleet.control.routes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.routes.commons.exception.dto.ErrorResponse;
import com.fleet.control.routes.commons.exception.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes the common error contract for rejections raised by the security chain. */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /**
   * Handles unauthenticated requests with the common error contract.
   *
   * @param request the current request
   * @param response the response to write
   * @param authException the authentication failure raised
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    Object marker = request.getAttribute(SecurityConstants.AUTH_ERROR_ATTRIBUTE);
    if (SecurityConstants.TOKEN_EXPIRED.equals(marker)) {
      write(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          ErrorCode.TOKEN_EXPIRED,
          "Token has expired");
      return;
    }
    write(
        response,
        HttpServletResponse.SC_UNAUTHORIZED,
        ErrorCode.UNAUTHORIZED,
        "Authentication required");
  }

  /**
   * Handles forbidden requests with the common error contract.
   *
   * @param request the current request
   * @param response the response to write
   * @param accessDeniedException the access denial raised
   */
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(
        response,
        HttpServletResponse.SC_FORBIDDEN,
        ErrorCode.FORBIDDEN,
        "You do not have permission to perform this action");
  }

  private void write(HttpServletResponse response, int status, ErrorCode error, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(error, message));
  }
}
