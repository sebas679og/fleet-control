package com.fleet.control.auth.config;

import com.fleet.control.auth.common.constants.HeaderConstants;
import com.fleet.control.auth.common.constants.JwtConstants;
import com.fleet.control.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);

    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }
    final String jwt = authHeader.substring(JwtConstants.BEARER_PREFIX.length());
    final String userEmail;

    try {
      userEmail = jwtService.extractEmail(jwt);
    } catch (Exception e) {
      log.warn("Token JWT inválido o expirado: {}", e.getMessage());
      filterChain.doFilter(request, response);
      return;
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // Cargamos el usuario completo desde la base de datos (incluyendo sus roles)
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

      if (!jwtService.isExpired(jwt)) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug("User {} success authenticated.", userDetails.getUsername());
      }
    }

    filterChain.doFilter(request, response);
  }
}
