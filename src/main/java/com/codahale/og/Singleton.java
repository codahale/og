package com.codahale.og;

import java.lang.annotation.*;

/**
 * An annotation indicating the result of a {@link Provides}-annotated method should be considered
 * a singleton.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Singleton {
}
