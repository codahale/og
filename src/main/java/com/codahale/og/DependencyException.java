package com.codahale.og;

import com.google.common.reflect.TypeToken;

public class DependencyException extends RuntimeException {
    public DependencyException(TypeToken<?> token, String name) {
        super(formatMessage(token, name));
    }


    public DependencyException(TypeToken<?> token, String name, Throwable cause) {
        super(formatMessage(token, name), cause);
    }

    private static String formatMessage(TypeToken<?> token, String name) {
        return "Unable to provide a " + token + (name == null ? "" : " named '" + name + '\'');
    }
}
