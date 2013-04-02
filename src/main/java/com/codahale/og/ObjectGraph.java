package com.codahale.og;

import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * An object graph. <p> Given a set of singletons and modules (which provide instances given
 * dependencies), constructs and memoizes instances of various types. </p>
 */
public class ObjectGraph {
    private final Map<BindingKey, Object> singletons;
    private final Map<BindingKey, Binding> entryPoints;

    /**
     * Creates a new object graph.
     */
    public ObjectGraph() {
        this.singletons = Maps.newHashMap();
        this.entryPoints = Maps.newHashMap();
        addSingleton(this);
    }

    /**
     * Adds an singleton.
     *
     * @param singleton a singleton of any type
     */
    public void addSingleton(Object singleton) {
        addSingleton(singleton, null);
    }

    /**
     * Adds a named singleton.
     *
     * @param singleton a singleton of any type
     * @param name      {@code singleton}'s name
     * @see Named
     */
    public void addSingleton(Object singleton, String name) {
        singletons.put(new BindingKey(TypeToken.of(singleton.getClass()), name), singleton);
    }

    /**
     * Adds a module with {@link Provides}-annotated methods that provide instances of objects.
     *
     * @param module a module with annotated methods
     * @see Provides
     */
    public void addModule(Object module) {
        for (Method method : module.getClass().getDeclaredMethods()) {
            final Provides provides = method.getAnnotation(Provides.class);
            final Named named = method.getAnnotation(Named.class);
            final Singleton singleton = method.getAnnotation(Singleton.class);
            if (provides != null) {
                method.setAccessible(true);
                entryPoints.put(new BindingKey(TypeToken.of(method.getGenericReturnType()),
                                               named == null ? null : named.value()),
                                new Binding(method, module, singleton != null));
            }
        }
    }

    /**
     * Iterates through all provider methods of all modules and preloads all providable types as
     * singletons.
     */
    public void preload() {
        for (Map.Entry<BindingKey, Binding> entry : entryPoints.entrySet()) {
            if (entry.getValue().isSingleton()) {
                final BindingKey key = entry.getKey();
                get(key.getType(), key.getName());
            }
        }
    }

    /**
     * Returns an instance of the given type with the given name.
     *
     * @param token a {@link TypeToken} of the given type
     * @param name  the name of the instance
     * @param <T>   the given type
     * @return an instance of the given type
     * @throws DependencyException if an instance of the type cannot be provided
     * @see Named
     */
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
                if (binding.isSingleton()) {
                    singletons.put(key, o);
                }
                return (T) o;
            }

            // check for bounded bindings
            for (Map.Entry<BindingKey, Binding> entry : entryPoints.entrySet()) {
                if (key.isAssignableFrom(entry.getKey())) {
                    final Object o = get(entry.getValue());
                    if (entry.getValue().isSingleton()) {
                        singletons.put(key, o);
                    }
                    return (T) o;
                }
            }
        } catch (Exception e) {
            throw new UnprovidableTypeException(token, name, e);
        }

        throw new UnprovidableTypeException(token, name);
    }

    /**
     * Returns an unnamed instance of the given type.
     *
     * @param token a {@link TypeToken} of the given type
     * @param <T>   the given type
     * @return an instance of the given type
     * @throws DependencyException if an instance of the type cannot be provided
     */
    public <T> T get(TypeToken<T> token) throws DependencyException {
        return get(token, null);
    }

    /**
     * Returns an unnamed instance of the given class.
     *
     * @param klass the given class
     * @param <T>   the given type
     * @return an instance of the given type
     * @throws DependencyException if an instance of the type cannot be provided
     */
    public <T> T get(Class<T> klass) throws DependencyException {
        return get(klass, null);
    }

    /**
     * Returns an instance of the given class with the given name.
     *
     * @param klass the given class
     * @param name  the name of the instance
     * @param <T>   the given type
     * @return an instance of the given type
     * @throws DependencyException if an instance of the type cannot be provided
     * @see Named
     */
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

