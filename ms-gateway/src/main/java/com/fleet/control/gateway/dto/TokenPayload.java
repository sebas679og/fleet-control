package com.fleet.control.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Token validation result forwarded to downstream services. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenPayload {
  private boolean valid;
  private String userId;
  private String username;
  private String role;
}
