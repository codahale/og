package com.codahale.og;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

public class ObjectGraph {
    private final Map<BindingKey, Object> singletons;
    private final Map<BindingKey, Binding> entryPoints;

    public ObjectGraph() {
        this.singletons = Maps.newHashMap();
        this.entryPoints = Maps.newHashMap();
        addSingleton(this);
    }

    public void addSingleton(Object singleton) {
        addSingleton(singleton, null);
    }

    public void addSingleton(Object singleton, String name) {
        singletons.put(new BindingKey(TypeToken.of(singleton.getClass()), name), singleton);
    }

    public void addModule(Object module) {
        for (Method method : module.getClass().getDeclaredMethods()) {
            final Provides provides = method.getAnnotation(Provides.class);
            final Named named = method.getAnnotation(Named.class);
            if (provides != null) {
                method.setAccessible(true);
                entryPoints.put(new BindingKey(TypeToken.of(method.getGenericReturnType()),
                                        named == null ? null : named.value()),
                                new Binding(method, module));
            }
        }
    }

    public void preload() {
        for (BindingKey key : entryPoints.keySet()) {
            get(key.getType(), key.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(TypeToken<T> token, String name) throws DependencyException {
        try {
            final BindingKey key = new BindingKey(token, name);

            // check for singletons
            final Object singleton = singletons.get(key);
            if (singleton != null) {
                return (T) singleton;
            }

            // check for exact bindings
            final Binding binding = entryPoints.get(key);
            if (binding != null) {
                final Object o = get(binding);
                singletons.put(key, o);
                return (T) o;
            }

            // check for bounded bindings
            for (Map.Entry<BindingKey, Binding> entry : entryPoints.entrySet()) {
                if (key.isAssignableFrom(entry.getKey())) {
                    final Object o = get(entry.getValue());
                    singletons.put(key, o);
                    return (T) o;
                }
            }
        } catch (Exception e) {
            throw new UnprovidableClassException(token, name, e);
        }

        throw new UnprovidableClassException(token, name);
    }

    public <T> T get(TypeToken<T> token) throws DependencyException {
        return get(token, null);
    }

    public <T> T get(Class<T> klass) throws DependencyException {
        return get(klass, null);
    }

    public <T> T get(Class<T> klass, String name) throws DependencyException {
        return get(TypeToken.of(klass), name);
    }

    private Object get(Binding binding) throws InvocationTargetException, IllegalAccessException {
        final Type[] parameterTypes = binding.getMethod().getGenericParameterTypes();
        final Annotation[][] annotations = binding.getMethod().getParameterAnnotations();
        final Object[] params = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            final Named named = findNames(annotations[i]);
            params[i] = ObjectGraph.this.get(TypeToken.of(parameterTypes[i]),
                                             named == null ? null : named.value());
        }
        return binding.getMethod().invoke(binding.getProvider(), params);
    }

    private Named findNames(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Named) {
                return (Named) annotation;
            }
        }
        return null;
    }
}

