package com.fleet.control.vehicles.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates service-to-service calls carrying the shared internal key.
 *
 * <p>A valid key grants the caller MANAGER permissions. A wrong key is
 * fail-closed with 401; an absent key lets the request continue so the JWT
 * filter or the entry point can decide.
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

    String provided = request.getHeader(SecurityConstants.INTERNAL_KEY_HEADER);

    if (!StringUtils.hasText(provided)) {
      filterChain.doFilter(request, response);
      return;
    }
    if (internalApiKey == null
        || internalApiKey.isBlank()
        || !internalApiKey.equals(provided)) {
      securityErrorHandler.commence(
          request, response, new BadCredentialsException("Invalid internal key"));
      return;
    }
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(
              "internal-service",
              null,
              List.of(
                  new SimpleGrantedAuthority(
                      SecurityConstants.ROLE_PREFIX + SecurityConstants.ROLE_MANAGER)));
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);
    }
    filterChain.doFilter(request, response);
  }
}
