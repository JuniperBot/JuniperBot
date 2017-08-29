package ru.caramel.juniperbot.utils;

import org.springframework.util.PropertyPlaceholderHelper;

import java.util.HashMap;
import java.util.Map;

public class MapPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

    private Map<String, String> values = new HashMap<>();

    public String put(String key, String value) {
        return values.put(key, value);
    }

    @Override
    public String resolvePlaceholder(String placeholderName) {
        return values.get(placeholderName);
    }
}
