package com.fleet.control.fuel.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKey;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Request filter that authenticates callers from a valid JWT issued by ms-auth.
 *
 * <p>Each domain service validates the token on its own: the HS256 signature is verified with the
 * shared secret and the role is read from the {@code role} claim. No database lookup is needed.
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

  private final SecretKey secretKey;

  /**
   * Creates the filter with the shared HS256 signing secret.
   *
   * @param secret signing secret, at least 32 characters
   */
  public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 characters.");
    }
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (!StringUtils.hasText(authHeader)
        || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwt = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());

    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(jwt).getPayload();

      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        String role = normalizeRole(claims.get(SecurityConstants.CLAIM_ROLE, String.class));
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                List.of(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role)));
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    } catch (JwtException | IllegalArgumentException e) {
      if (log.isDebugEnabled()) {
        log.debug("Invalid or expired JWT: {}", e.getMessage());
      }
      request.setAttribute(
          SecurityConstants.AUTH_ERROR_ATTRIBUTE,
          isExpiredCause(e) ? SecurityConstants.TOKEN_EXPIRED : null);
    }

    filterChain.doFilter(request, response);
  }

  private static String normalizeRole(String role) {
    if (role == null || role.isBlank()) {
      return SecurityConstants.ROLE_MANAGER;
    }
    String normalized = role.toUpperCase(Locale.ROOT);
    if (normalized.startsWith(SecurityConstants.ROLE_PREFIX)) {
      normalized = normalized.substring(SecurityConstants.ROLE_PREFIX.length());
    }
    return normalized;
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
