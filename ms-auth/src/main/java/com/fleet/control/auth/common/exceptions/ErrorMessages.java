package com.fleet.control.auth.common.exceptions;

/** Human-readable messages for controlled API errors. */
public class ErrorMessages {

  private ErrorMessages() {}

  public static final String INVALID_CREDENTIALS = "Invalid credentials";
  public static final String UNAUTHORIZED = "Missing or invalid authentication token";
  public static final String FORBIDDEN = "Access denied: insufficient permissions";
  public static final String USER_ALREADY_EXISTS = "A user with that email already exists";
  public static final String USERNAME_ALREADY_EXISTS = "A user with that username already exists";
  public static final String VALIDATION_FAILED = "Validation failed";
  public static final String TOKEN_EXPIRED = "The token has expired";
  public static final String INVALID_TOKEN = "The token is invalid";
  public static final String MISSING_AUTHORIZATION_HEADER =
      "Missing required header: Authorization";
}
