package com.codahale.og;

/**
 * The base class for all Og exceptions.
 */
public abstract class DependencyException extends RuntimeException {
    protected DependencyException(String message) {
        super(message);
    }

    protected DependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
