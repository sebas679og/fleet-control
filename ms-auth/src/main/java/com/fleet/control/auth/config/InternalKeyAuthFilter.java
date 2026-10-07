package com.fleet.control.auth.config;

import com.fleet.control.auth.common.constants.HeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards internal service-to-service endpoints with a shared key.
 *
 * <p>Fail-closed: a missing or wrong key answers 401 through the security error handler without
 * continuing the chain, so a valid JWT alone can never bypass the internal gate. Never throws.
 */
@Component
public class InternalKeyAuthFilter extends OncePerRequestFilter {

  private final SecurityErrorHandler securityErrorHandler;
  private final String internalApiKey;

  /**
   * Creates the filter with the error handler and the configured shared key.
   *
   * @param securityErrorHandler writes the 401 contract for rejected requests
   * @param internalApiKey the expected {@code X-Internal-Key} value
   */
  public InternalKeyAuthFilter(
      SecurityErrorHandler securityErrorHandler,
      @Value("${internal.api-key}") String internalApiKey) {
    this.securityErrorHandler = securityErrorHandler;
    this.internalApiKey = internalApiKey;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    if (!"/internal".equals(path) && !path.startsWith("/internal/")) {
      filterChain.doFilter(request, response);
      return;
    }
    String provided = request.getHeader(HeaderConstants.INTERNAL_KEY);
    if (internalApiKey == null || internalApiKey.isBlank() || !internalApiKey.equals(provided)) {
      securityErrorHandler.commence(
          request, response, new BadCredentialsException("Invalid internal key"));
      return;
    }
    filterChain.doFilter(request, response);
  }
}
