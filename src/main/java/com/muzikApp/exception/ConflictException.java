package com.muzikApp.exception;

/** 409: e.g. the e-mail is taken, or the song is already in the playlist. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
