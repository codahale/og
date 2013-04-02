package com.codahale.og;

import java.lang.reflect.Method;

class Binding {
    private final Method method;
    private final Object provider;
    private final boolean singleton;

    Binding(Method method, Object provider, boolean singleton) {
        this.method = method;
        this.provider = provider;
        this.singleton = singleton;
    }

    Object getProvider() {
        return provider;
    }

    Method getMethod() {
        return method;
    }

    boolean isSingleton() {
        return singleton;
    }
}
