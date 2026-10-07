package com.fleet.control.auth.config;

import com.fleet.control.auth.common.constants.JwtConstants;
import com.fleet.control.auth.common.exceptions.ErrorCodes;
import com.fleet.control.auth.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Request filter that authenticates callers from a valid JWT. */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // Intentional: filter must not fail.
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }
    final String jwt = authHeader.substring(JwtConstants.BEARER_PREFIX.length());
    final String userEmail;

    try {
      userEmail = jwtService.extractEmail(jwt);
    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invalid or expired JWT: {}", e.getMessage());
      }
      // An expired token throws ExpiredJwtException from the parser, so a valid
      // email here always means a live token and no further expiry check is needed.
      request.setAttribute(
          SecurityErrorHandler.AUTH_ERROR_ATTRIBUTE,
          isExpiredCause(e) ? ErrorCodes.TOKEN_EXPIRED : ErrorCodes.UNAUTHORIZED);
      filterChain.doFilter(request, response);
      return;
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // Load the full user from the database (including roles)
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);
  }

  private static boolean isExpiredCause(Throwable throwable) {
    Throwable cause = throwable;
    while (cause != null) {
      if (cause instanceof ExpiredJwtException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }
}
