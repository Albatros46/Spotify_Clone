package com.muzikApp.exception;

/** 400: the request is well-formed but not acceptable (e.g. wrong file type). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
