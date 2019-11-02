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
package ru.juniperbot.worker.commands.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.modules.moderation.model.ModerationActionRequest;
import ru.juniperbot.common.worker.modules.moderation.model.WarningResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@DiscordCommand(key = "discord.command.mod.warn.key",
        description = "discord.command.mod.warn.desc",
        group = "discord.command.group.moderation",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.BAN_MEMBERS},
        priority = 5)
public class WarnCommand extends MentionableModeratorCommand {

    public WarnCommand() {
        super(false, true);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        Member violator = reference.getMember();
        LocalMember localMember = reference.getLocalMember();
        if (!checkTarget(reference, event)) {
            return false;
        }

        List<Role> currentRoles = violator != null ? new ArrayList<>(violator.getRoles()) : null;
        WarningResult result = moderationService.warn(event.getMember(), violator, localMember, query);

        StringBuilder argumentBuilder = new StringBuilder();

        if (result.isReset()) {
            argumentBuilder
                    .append("\n")
                    .append(messageService.getMessage("discord.command.mod.warn.reset"));
        }

        if (violator != null && result.isPunished()) {
            ModerationActionRequest request = result.getRequest();

            switch (request.getType()) {
                case MUTE:
                    if (request.getDuration() != null) {
                        argumentBuilder
                                .append("\n")
                                .append(getMuteDuration(request.getDuration()));
                    }
                    break;
                case CHANGE_ROLES:
                    List<Role> assignedRoles = getRoles(event.getGuild(), request.getAssignRoles());
                    assignedRoles.removeAll(currentRoles);
                    if (CollectionUtils.isNotEmpty(assignedRoles)) {
                        String assignedMentions = assignedRoles.stream()
                                .map(Role::getAsMention)
                                .collect(Collectors.joining(", "));
                        argumentBuilder
                                .append("\n")
                                .append(messageService.getMessage("discord.command.mod.warn.exceeded.message.CHANGE_ROLES.assignedRoles",
                                        assignedMentions));
                    }

                    List<Role> revokedRoles = getRoles(event.getGuild(), request.getRevokeRoles());
                    revokedRoles.removeIf(e -> !currentRoles.contains(e));
                    if (CollectionUtils.isNotEmpty(revokedRoles)) {
                        String revokedMentions = revokedRoles.stream()
                                .map(Role::getAsMention)
                                .collect(Collectors.joining(", "));
                        argumentBuilder
                                .append("\n")
                                .append(messageService.getMessage("discord.command.mod.warn.exceeded.message.CHANGE_ROLES.revokedRoles",
                                        revokedMentions));
                    }

                    break;
            }

            String messageCode = "discord.command.mod.warn.exceeded.message." + request.getType().name();
            messageService.onEmbedMessage(event.getChannel(), messageCode, localMember.getEffectiveName(),
                    result.getNumber(), argumentBuilder.toString());
            return false;
        }
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.message",
                localMember.getEffectiveName(), result.getNumber(), argumentBuilder.toString());
        return true;
    }

    private List<Role> getRoles(Guild guild, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return roleIds.stream()
                .map(guild::getRoleById)
                .filter(e -> e != null && guild.getSelfMember().canInteract(e))
                .collect(Collectors.toList());
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String warnCommand = messageService.getMessageByLocale("discord.command.mod.warn.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.help",
                context.getConfig().getPrefix(), warnCommand);
    }
}
