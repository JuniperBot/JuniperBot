/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.model.discord;

import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.common.model.FeatureSet;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class GuildDto implements Serializable {
    private static final long serialVersionUID = 5728172690699536067L;

    public static final GuildDto EMPTY = new GuildDto();

    private String id;

    private String name;

    private String iconUrl;

    private boolean available;

    private List<RoleDto> roles;

    private List<TextChannelDto> textChannels;

    private List<VoiceChannelDto> voiceChannels;

    private List<EmoteDto> emotes;

    private Set<FeatureSet> featureSets;

    private String defaultMusicChannelId;

    private long onlineCount;

}
