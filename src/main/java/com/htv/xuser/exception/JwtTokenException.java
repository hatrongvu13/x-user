package com.htv.xuser.exception;

/**
 * JwtTokenException — ném khi JWT invalid, expired, hoặc sai loại.
 * GlobalExceptionHandler map về HTTP 401.
 */
public class JwtTokenException extends RuntimeException {

    public JwtTokenException(String message) {
        super(message);
    }

    public JwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
