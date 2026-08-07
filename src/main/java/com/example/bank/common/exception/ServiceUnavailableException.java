package com.example.bank.common.exception;

/** Raised when a required downstream service cannot be called. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
