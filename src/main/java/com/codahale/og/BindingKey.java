package com.codahale.og;

import com.google.common.reflect.TypeToken;

class BindingKey {
    private final TypeToken<?> type;
    private final String name;

    BindingKey(TypeToken<?> type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null || getClass() != obj.getClass()) { return false; }
        final BindingKey key = (BindingKey) obj;
        return !(getName() != null ? !getName().equals(key.getName()) : key.getName() != null) &&
                getType().equals(key.getType());
    }

    @Override
    public int hashCode() {
        return 31 * getType().hashCode() + (getName() != null ? getName().hashCode() : 0);
    }

    @Override
    public String toString() {
        return getType().toString() + (getName() == null ? "" : '/' + getName());
    }

    TypeToken<?> getType() {
        return type;
    }

    String getName() {
        return name;
    }

    boolean isAssignableFrom(BindingKey key) {
        return type.isAssignableFrom(key.type) && (name == null || name.equals(key.name));
    }
}
