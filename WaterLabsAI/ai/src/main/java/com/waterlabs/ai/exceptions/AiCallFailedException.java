package com.waterlabs.ai.exceptions;

public class AiCallFailedException extends RuntimeException {

    public AiCallFailedException(String message) {
        super(message);
    }

    public AiCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
