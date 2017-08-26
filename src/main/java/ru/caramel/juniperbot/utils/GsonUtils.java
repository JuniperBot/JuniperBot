package ru.caramel.juniperbot.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Field;

public final class GsonUtils {

    private GsonUtils() {
        // private
    }

    public static Gson create() {
        GsonBuilder builder = new GsonBuilder();
        builder.addDeserializationExclusionStrategy(new SuperclassExclusionStrategy());
        builder.addSerializationExclusionStrategy(new SuperclassExclusionStrategy());
        return builder.create();
    }

    public static class SuperclassExclusionStrategy implements ExclusionStrategy {
        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            String fieldName = fieldAttributes.getName();
            Class<?> theClass = fieldAttributes.getDeclaringClass();

            return isFieldInSuperclass(theClass, fieldName);
        }

        private boolean isFieldInSuperclass(Class<?> subclass, String fieldName) {
            Class<?> superclass = subclass.getSuperclass();
            Field field;

            while (superclass != null) {
                field = getField(superclass, fieldName);

                if (field != null)
                    return true;

                superclass = superclass.getSuperclass();
            }

            return false;
        }

        private Field getField(Class<?> theClass, String fieldName) {
            try {
                return theClass.getDeclaredField(fieldName);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
