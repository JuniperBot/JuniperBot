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
package ru.caramel.juniperbot.web.dto.config;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
public class MusicConfigDto implements Serializable {

    private String channelId;

    private boolean userJoinEnabled;

    private String textChannelId;

    @Size(max = 255)
    private String autoPlay;

    private boolean playlistEnabled;

    private boolean streamsEnabled;

    @Min(0)
    private Long queueLimit;

    @Min(0)
    private Long durationLimit;

    @Min(0)
    private Long duplicateLimit;

    private Set<String> roles;
}
