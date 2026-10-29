package com.muzikApp.exception;

/** 404: the resource does not exist, or the caller may not see it. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
