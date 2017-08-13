package ru.caramel.juniperbot.security.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractDetails implements Serializable {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    protected String id;

    @SuppressWarnings("unchecked")
    protected static  <T> void setValue(Class<T> type, Map<Object, Object> map, String name, Consumer<T> setter) {
        Object value = map.get(name);
        if (value == null) {
            return;
        }
        if (!type.isAssignableFrom(value.getClass())) {
            throw new IllegalStateException(String.format("Wrong user details class type for %s. Found [%s], expected [%s]",
                    name, value.getClass().getName(), type.getName()));
        }
        setter.accept((T) value);
    }
}
