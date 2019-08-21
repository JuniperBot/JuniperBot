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
package ru.juniperbot.common.service;

import net.dv8tion.jda.api.entities.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import ru.juniperbot.common.model.discord.*;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DiscordMapperService {

    @Mappings({
            @Mapping(target = "featureSets", ignore = true),
            @Mapping(target = "defaultMusicChannelId", ignore = true),
            @Mapping(target = "onlineCount", ignore = true)
    })
    GuildDto getGuildDto(Guild guild);

    @Mappings({
            @Mapping(expression = "java(role.getGuild().getSelfMember().canInteract(role))", target = "interactable"),
    })
    RoleDto getRoleDto(Role role);

    EmoteDto getEmoteDto(Emote role);

    @Mappings({
            @Mapping(expression = "java(webhook.getIdLong())", target = "webhookId"),
            @Mapping(expression = "java(webhook.getGuild().getIdLong())", target = "guildId"),
            @Mapping(expression = "java(webhook.getChannel().getId())", target = "channelId"),
            @Mapping(expression = "java(webhook.getDefaultUser().getAvatarUrl())", target = "iconUrl"),
            @Mapping(target = "id", ignore = true)
    })
    WebhookDto getWebhookDto(Webhook webhook);

    @Mappings({
            @Mapping(source = "NSFW", target = "nsfw"),
            @Mapping(expression = "java(channel.canTalk())", target = "canTalk"),
            @Mapping(expression = "java(net.dv8tion.jda.api.Permission.getRaw(channel.getGuild().getSelfMember().getPermissions(channel)))", target = "permissions"),
    })
    TextChannelDto getTextChannelDto(TextChannel channel);

    List<TextChannelDto> getTextChannelDto(List<TextChannel> channels);

    @Mappings({
            @Mapping(expression = "java(net.dv8tion.jda.api.Permission.getRaw(channel.getGuild().getSelfMember().getPermissions(channel)))", target = "permissions")
    })
    VoiceChannelDto getVoiceChannelDto(VoiceChannel channel);

    List<VoiceChannelDto> getVoiceChannelDto(List<VoiceChannel> channels);

    List<EmoteDto> getEmotesDto(Collection<Emote> role);

}
