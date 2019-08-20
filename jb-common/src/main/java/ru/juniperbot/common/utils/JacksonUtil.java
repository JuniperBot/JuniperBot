/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacksonUtil {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T fromString(String string, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(string, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + string + " cannot be transformed to Json object");
        }
    }

    public static String toString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + value + " cannot be transformed to a String");
        }
    }

    public static JsonNode toJsonNode(String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T value) {
        return fromString(toString(value), (Class<T>) value.getClass());
    }

    public static List mapJsonToObjectList(String json, Class clazz) {
        List list;
        TypeFactory t = TypeFactory.defaultInstance();
        try {
            list = OBJECT_MAPPER.readValue(json, t.constructCollectionType(ArrayList.class, clazz));
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + json + " cannot be transformed to List of " + clazz.getName());
        }

        return list;
    }

    public static Map mapJsonToMap(String json, Class keyClass, Class valueClass) {
        Map map;
        TypeFactory t = TypeFactory.defaultInstance();
        try {
            map = OBJECT_MAPPER.readValue(json, t.constructMapType(HashMap.class, keyClass, valueClass));
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + json + " cannot be transformed to Map<" + keyClass.getName() + ", " + valueClass.getName() + ">.");
        }
        return map;
    }
}