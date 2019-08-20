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
package ru.juniperbot.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InviteInfo implements Serializable {

    private static final long serialVersionUID = 706922461566551000L;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Inviter {
        private String username;
        private String discriminator;
        private String id;
        private String avatar;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Guild {
        private String splash;
        private String[] features;
        private String name;
        @JsonProperty("verification_level")
        private Integer verificationLevel;
        private String icon;
        private String banner;
        private String id;
        @JsonProperty("vanity_url_code")
        private String vanityUrlCode;
        private String description;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String name;
        private Integer type;
        private String id;
    }

    private Inviter inviter;

    private String code;

    private Guild guild;

    private Channel channel;

    private String message;
}
