package com.example.stockreplenishment.exception;

// Thrown when a business rule forbids an operation, for example deleting a category when that still holds products.
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }

}
