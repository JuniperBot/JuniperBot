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
package ru.juniperbot.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.common.model.FeatureSet;
import ru.juniperbot.common.model.discord.RoleDto;
import ru.juniperbot.common.model.discord.TextChannelDto;
import ru.juniperbot.common.model.discord.VoiceChannelDto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder(builderClassName = "Builder")
public class GuildInfoDto implements Serializable {
    private static final long serialVersionUID = 5728172690699536067L;

    private String id;

    private String name;

    private String prefix;

    private String locale;

    private String color;

    private String commandLocale;

    private String icon;

    private String timeZone;

    private boolean available;

    private List<RoleDto> roles;

    private List<TextChannelDto> textChannels;

    private List<VoiceChannelDto> voiceChannels;

    private Set<FeatureSet> featureSets;

}
