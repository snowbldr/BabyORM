package com.babyorm;

public class BabyDBException extends RuntimeException {
    public BabyDBException(String message) {
        super(message);
    }

    public BabyDBException(String message, Throwable cause) {
        super(message, cause);
    }
}
