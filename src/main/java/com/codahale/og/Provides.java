package com.codahale.og;

import java.lang.annotation.*;

/**
 * An annotation indicating a provider method of a module.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Provides {

}
