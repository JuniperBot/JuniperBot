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
package ru.juniperbot.common.model.patreon.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SocialConnections {

    private UserIdObject youtube;
    private UserIdObject twitter;
    private UserIdObject deviantart;
    private UserIdObject discord;
    private UserIdObject twitch;
    private UserIdObject facebook;
    private UserIdObject spotify;
    private UserIdObject reddit;

    @Getter
    @Setter
    public static class UserIdObject {
        private String user_id;
        private List<String> scopes;
        private String url;
    }
}
