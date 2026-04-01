package com.deliverytracking.exception;

public class DuplicateTrackingIdException extends RuntimeException {
    public DuplicateTrackingIdException(String message) {
        super(message);
    }
}
