package com.muzikApp.exception;

/** 401: missing, expired, forged or already used token. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
