package com.codahale.og;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;

/**
 * This exception is thrown when Og is unable to provide an instance of a class.
 */
public class UnprovidableTypeException extends DependencyException {
    private final TypeToken<?> typeToken;
    private final String name;

    UnprovidableTypeException(TypeToken<?> typeToken, String name) {
        super(formatMessage(typeToken, name));
        this.typeToken = typeToken;
        this.name = name;
    }

    UnprovidableTypeException(TypeToken<?> typeToken, String name, Throwable cause) {
        super(formatMessage(typeToken, name), cause);
        this.typeToken = typeToken;
        this.name = name;
    }

    /**
     * Returns a {@link TypeToken} of the expected type.
     *
     * @return a {@link TypeToken} of the expected type
     */
    @SuppressWarnings("UnusedDeclaration")
    public TypeToken<?> getTypeToken() {
        return typeToken;
    }

    /**
     * Returns the name, if any, of the expected type.
     *
     * @return the name, if any, of the expected type
     */
    @SuppressWarnings("UnusedDeclaration")
    public Optional<String> getName() {
        return Optional.fromNullable(name);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("token", typeToken).add("name", name).toString();
    }

    private static String formatMessage(TypeToken<?> token, String name) {
        return "Unable to provide a " + token + (name == null ? "" : " named '" + name + '\'');
    }
}
