package com.fleet.control.auth.service;

import com.fleet.control.auth.models.dto.token.request.TokenPayload;
import com.fleet.control.auth.models.dto.token.response.TokenResponse;
import io.jsonwebtoken.Claims;

public interface JwtService {

  TokenResponse generateToken(String email, String userId, String username, String role);

  TokenPayload validateToken(String token);

  Claims getClaims(String token);

  boolean isExpired(String token);

  String extractRole(String token);

  String extractEmail(String token);
}
