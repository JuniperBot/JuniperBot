package ru.caramel.juniperbot.security.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.utils.Constants;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

public class DiscordUserDetails implements Serializable {

    private static final long serialVersionUID = -7122411547956564478L;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String userName;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private Boolean verified;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private Boolean mfaEnabled;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String id;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String avatar;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String discriminator;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String email;

    public String getAvatarUrl() {
        return String.format(Constants.DISCORD_USER_AVATAR_URL_FORMAT, id, avatar);
    }

    public static DiscordUserDetails create(Map<String, Object> map) {
        DiscordUserDetails details = new DiscordUserDetails();
        setValue(String.class, map, "username", details::setUserName);
        setValue(Boolean.class, map, "verified", details::setVerified);
        setValue(Boolean.class, map, "mfa_enabled", details::setMfaEnabled);
        setValue(String.class, map, "id", details::setId);
        setValue(String.class, map, "avatar", details::setAvatar);
        setValue(String.class, map, "discriminator", details::setDiscriminator);
        setValue(String.class, map, "email", details::setEmail);
        return details;
    }

    @SuppressWarnings("unchecked")
    private static  <T> void setValue(Class<T> type, Map<String, Object> map, String name, Consumer<T> setter) {
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
