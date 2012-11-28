package com.codahale.og;

import java.lang.annotation.*;

/**
 * Indicates that the return value or parameter of a provider method must have a specific name.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Named {
    /**
     * The name of the provided type.
     */
    String value();
}
