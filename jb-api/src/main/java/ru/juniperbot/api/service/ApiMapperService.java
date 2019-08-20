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
package ru.juniperbot.api.service;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import ru.juniperbot.api.dto.*;
import ru.juniperbot.api.dto.config.*;
import ru.juniperbot.api.dto.games.ReactionRouletteDto;
import ru.juniperbot.api.dto.playlist.PlaylistDto;
import ru.juniperbot.api.dto.playlist.PlaylistItemDto;
import ru.juniperbot.api.security.model.DiscordGuildDetails;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.*;
import ru.juniperbot.common.persistence.entity.base.NamedReference;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ApiMapperService {

    @Mappings({
            @Mapping(target = "added", ignore = true),
            @Mapping(target = "members", ignore = true)
    })
    GuildShortDto getGuildDto(DiscordGuildDetails details);

    List<GuildShortDto> getGuildDtos(List<DiscordGuildDetails> details);

    CommonConfigDto getCommonDto(GuildConfig config);

    @Mappings({
            @Mapping(target = "prefix", expression = "java(trimmed(source.getPrefix()))"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "name", ignore = true),
            @Mapping(target = "iconUrl", ignore = true)
    })
    void updateCommon(CommonConfigDto source, @MappingTarget GuildConfig target);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getAssignRoles()))", target = "assignRoles"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getRevokeRoles()))", target = "revokeRoles"),
    })
    ModerationActionDto getModerationActionDto(ModerationAction source);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "config", ignore = true),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getAssignRoles()))", target = "assignRoles"),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getRevokeRoles()))", target = "revokeRoles"),
    })
    void updateModerationAction(ModerationActionDto source, @MappingTarget ModerationAction target);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getRoles()))", target = "roles"),
    })
    ModerationConfigDto getModerationDto(ModerationConfig source);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "mutedRoleId", ignore = true),
            @Mapping(target = "actions", ignore = true),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getRoles()))", target = "roles"),
    })
    void updateModerationConfig(ModerationConfigDto source, @MappingTarget ModerationConfig target);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toString(source.getChannelId()))", target = "channelId"),
            @Mapping(expression = "java(ApiMapperService.toString(source.getTextChannelId()))", target = "textChannelId"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getRoles()))", target = "roles"),
    })
    MusicConfigDto getMusicDto(MusicConfig source);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "voiceVolume", ignore = true),
            @Mapping(expression = "java(ApiMapperService.toLong(source.getChannelId()))", target = "channelId"),
            @Mapping(expression = "java(ApiMapperService.toLong(source.getTextChannelId()))", target = "textChannelId"),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getRoles()))", target = "roles"),
    })
    void updateMusicConfig(MusicConfigDto source, @MappingTarget MusicConfig target);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getIgnoredChannels()))", target = "ignoredChannels")
    })
    RankingDto getRankingDto(RankingConfig source);

    RankingInfoDto getRankingInfoDto(RankingInfo info);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getJoinRoles()))", target = "joinRoles"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getRestoreRoles()))", target = "restoreRoles"),
    })
    WelcomeDto getWelcomeDto(WelcomeMessage source);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "joinTemplate", ignore = true),
            @Mapping(target = "joinDmTemplate", ignore = true),
            @Mapping(target = "leaveTemplate", ignore = true),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getJoinRoles()))", target = "joinRoles"),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getRestoreRoles()))", target = "restoreRoles"),
    })
    void updateWelcome(WelcomeDto source, @MappingTarget WelcomeMessage target);

    AuditConfigDto getAuditConfigDto(AuditConfig source);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true)
    })
    void updateAudit(AuditConfigDto source, @MappingTarget AuditConfig target);

    @Mappings({
            @Mapping(target = "enabled", ignore = true),
            @Mapping(target = "allowedRoles", ignore = true),
            @Mapping(target = "ignoredRoles", ignore = true),
            @Mapping(target = "allowedChannels", ignore = true),
            @Mapping(target = "ignoredChannels", ignore = true),
            @Mapping(target = "deleteSource", ignore = true),
            @Mapping(target = "coolDown", ignore = true),
            @Mapping(target = "coolDownMode", ignore = true),
            @Mapping(target = "coolDownIgnoredRoles", ignore = true)
    })
    CustomCommandDto getCustomCommandDto(CustomCommand command);

    List<CustomCommandDto> getCustomCommandsDto(List<CustomCommand> command);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(target = "commandConfig", ignore = true),
            @Mapping(target = "messageTemplate", ignore = true)
    })
    void updateCustomCommand(CustomCommandDto source, @MappingTarget CustomCommand target);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getIgnoredChannels()))", target = "ignoredChannels")
    })
    ReactionRouletteDto getReactionRouletteDto(ReactionRoulette source);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getIgnoredChannels()))", target = "ignoredChannels"),
    })
    void updateReactionRoulette(ReactionRouletteDto source, @MappingTarget ReactionRoulette target);

    PlaylistItemDto getPlaylistItemDto(PlaylistItem source);

    List<PlaylistItemDto> getPlaylistItemDtos(List<PlaylistItem> source);

    NamedReferenceDto getNamedReferenceDto(NamedReference source);

    AuditActionDto getAuditActionDto(AuditAction source);

    List<AuditActionDto> getAuditActionDtos(List<AuditAction> source);

    @Mappings({
            @Mapping(target = "guild", ignore = true)
    })
    PlaylistDto getPlaylistDto(Playlist playlist);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getAllowedRoles()))", target = "allowedRoles"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getIgnoredRoles()))", target = "ignoredRoles"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getAllowedChannels()))", target = "allowedChannels"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getIgnoredChannels()))", target = "ignoredChannels"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getCoolDownIgnoredRoles()))", target = "coolDownIgnoredRoles"),
            @Mapping(expression = "java(!source.isDisabled())", target = "enabled")
    })
    CommandDto getCommandDto(CommandConfig source);

    @Mappings({
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getAllowedRoles()))", target = "allowedRoles"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getIgnoredRoles()))", target = "ignoredRoles"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getAllowedChannels()))", target = "allowedChannels"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getIgnoredChannels()))", target = "ignoredChannels"),
            @Mapping(expression = "java(ApiMapperService.toStringSet(source.getCoolDownIgnoredRoles()))", target = "coolDownIgnoredRoles"),
            @Mapping(expression = "java(!source.isDisabled())", target = "enabled")
    })
    void updateCommandDto(CommandConfig source, @MappingTarget CommandDto target);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "guildId", ignore = true),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getAllowedRoles()))", target = "allowedRoles"),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getIgnoredRoles()))", target = "ignoredRoles"),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getAllowedChannels()))", target = "allowedChannels"),
            @Mapping(expression = "java(ApiMapperService.toLongList(source.getIgnoredChannels()))", target = "ignoredChannels"),
            @Mapping(expression = "java(!source.isEnabled())", target = "disabled")
    })
    void updateCommandConfig(CommandDto source, @MappingTarget CommandConfig target);

    MessageTemplateDto getTemplateDto(MessageTemplate template);

    MessageTemplateFieldDto getTemplateFieldDto(MessageTemplateField templateField);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "fields", ignore = true)
    })
    void updateTemplate(MessageTemplateDto source, @MappingTarget MessageTemplate target);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "template", ignore = true),
            @Mapping(target = "index", ignore = true)
    })
    MessageTemplateField getTemplateField(MessageTemplateFieldDto source);

    List<MessageTemplateField> getTemplateFields(List<MessageTemplateFieldDto> source);

    default String trimmed(String s) {
        return s != null ? s.trim() : null;
    }

    static String toString(Long source) {
        return source != null ? String.valueOf(source) : null;
    }

    static Long toLong(String source) {
        return StringUtils.isNumeric(source) ? Long.valueOf(source) : null;
    }

    static Set<String> toStringSet(Collection<Long> source) {
        return source != null ? source.stream().map(String::valueOf).collect(Collectors.toSet()) : null;
    }

    static List<Long> toLongList(Collection<String> source) {
        return source != null ? source
                .stream()
                .filter(StringUtils::isNumeric)
                .map(Long::valueOf).collect(Collectors.toList()) : null;
    }
}
