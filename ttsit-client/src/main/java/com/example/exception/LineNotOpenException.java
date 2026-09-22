package com.example.exception;

/** Thrown when the audio line is used before it was opened, or after it was closed. */
public class LineNotOpenException extends RuntimeException {

    public LineNotOpenException(String message) {
        super(message);
    }
}
