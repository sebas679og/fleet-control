package com.fleet.control.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.common.exceptions.ErrorMessages;
import com.fleet.control.auth.models.dto.exceptions.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes the common error contract for rejections raised by the security chain. */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  /**
   * Request attribute used by filters to report the machine-readable error code. When it holds
   * {@code TOKEN_EXPIRED}, the entry point answers 401 with that code instead of the default {@code
   * UNAUTHORIZED}.
   */
  public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    Object marker = request.getAttribute(AUTH_ERROR_ATTRIBUTE);
    if (ErrorCodes.TOKEN_EXPIRED.equals(marker)) {
      write(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          ErrorCodes.TOKEN_EXPIRED,
          ErrorMessages.TOKEN_EXPIRED);
      return;
    }
    write(
        response,
        HttpServletResponse.SC_UNAUTHORIZED,
        ErrorCodes.UNAUTHORIZED,
        ErrorMessages.UNAUTHORIZED);
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
        response, HttpServletResponse.SC_FORBIDDEN, ErrorCodes.FORBIDDEN, ErrorMessages.FORBIDDEN);
  }

  private void write(HttpServletResponse response, int status, String error, String message)
      throws IOException {
    ErrorResponse errorResponse =
        ErrorResponse.builder().error(error).message(message).timestamp(Instant.now()).build();
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), errorResponse);
  }
}
