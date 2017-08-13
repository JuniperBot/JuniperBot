package ru.caramel.juniperbot.security.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.integration.discord.model.AvatarType;

import java.util.Map;

public class DiscordUserDetails extends AbstractDetails {

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
    private String avatar;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String discriminator;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String email;

    public String getAvatarUrl() {
        return AvatarType.USER.getUrl(id, avatar);
    }

    public static DiscordUserDetails create(Map<Object, Object> map) {
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
}
