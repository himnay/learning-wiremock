package com.learnwiremock.exception;

public class MovieErrorResponseException extends RuntimeException {

    public MovieErrorResponseException(Throwable cause) {
        super(cause);
    }

    public MovieErrorResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
