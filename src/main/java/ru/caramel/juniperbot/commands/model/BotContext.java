package ru.caramel.juniperbot.commands.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class BotContext {

    private Guild guild;

    private String prefix;

    private GuildConfig config;

    @Getter(AccessLevel.PRIVATE)
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(Class<T> type, String key) {
        Object value = getAttribute(key);
        return value != null && type.isAssignableFrom(value.getClass()) ? (T) value : null;
    }

    public Object putAttribute(String key, Object value) {
        return attributes.put(key, value);
    }

    public Object removeAttribute(String key) {
        return attributes.get(key);
    }

    public <T> T removeAttribute(Class<T> type, String key) {
        Object value = removeAttribute(key);
        return value != null && type.isAssignableFrom(value.getClass()) ? (T) value : null;
    }
}
