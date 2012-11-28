package com.codahale.og;

import java.lang.reflect.Method;

class Binding {
    private final Method method;
    private final Object provider;

    Binding(Method method, Object provider) {
        this.method = method;
        this.provider = provider;
    }

    public Object getProvider() {
        return provider;
    }

    public Method getMethod() {
        return method;
    }
}
