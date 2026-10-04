package com.fleet.control.auth.common.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
    super(message);
    }
}
