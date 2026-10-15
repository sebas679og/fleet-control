package com.fleet.control.auth.common.exceptions.enums;

/** Machine-readable identifiers for controlled API errors. */
public enum ErrorCode {
  VALIDATION_ERROR,
  INVALID_CREDENTIALS,
  USER_ALREADY_EXISTS,
  UNAUTHORIZED,
  TOKEN_EXPIRED,
  INVALID_TOKEN,
  FORBIDDEN,
  INTERNAL_ERROR
}
