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
package ru.juniperbot.api.security.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

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
