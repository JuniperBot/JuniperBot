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
package ru.caramel.juniperbot.core.moderation.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;
import org.ocpsoft.prettytime.units.Second;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.core.moderation.model.ModerationActionRequest;
import ru.caramel.juniperbot.core.moderation.model.WarningResult;

import java.util.*;
import java.util.stream.Collectors;

@DiscordCommand(key = "discord.command.mod.warn.key",
        description = "discord.command.mod.warn.desc",
        group = "discord.command.group.moderation",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.BAN_MEMBERS},
        priority = 5)
public class WarnCommand extends ModeratorCommandAsync {

    @Override
    public void doCommandAsync(GuildMessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (mentioned == null) {
            String warnCommand = messageService.getMessageByLocale("discord.command.mod.warn.key",
                    context.getCommandLocale());
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.help",
                    context.getConfig().getPrefix(), warnCommand);
            return;
        }
        if (moderationService.isModerator(mentioned) || Objects.equals(mentioned, event.getMember())) {
            fail(event); // do not allow ban members or yourself
            return;
        }

        List<Role> currentRoles = new ArrayList<>(mentioned.getRoles());
        WarningResult result = moderationService.warn(event.getMember(), mentioned, removeMention(query));

        StringBuilder argumentBuilder = new StringBuilder();

        if (result.isReset()) {
            argumentBuilder
                    .append("\n")
                    .append(messageService.getMessage("discord.command.mod.warn.reset"));
        }

        if (result.isPunished()) {
            ModerationActionRequest request = result.getRequest();

            switch (request.getType()) {
                case MUTE:
                    Date date = new Date();
                    date.setTime(date.getTime() + (long) (60000 * request.getDuration()));
                    PrettyTime formatter = new PrettyTime(contextService.getLocale());
                    formatter.removeUnit(JustNow.class);
                    formatter.removeUnit(Millisecond.class);
                    formatter.removeUnit(Second.class);
                    argumentBuilder
                            .append("\n")
                            .append(messageService.getMessage("discord.command.mod.warn.exceeded.message.MUTE.until",
                                    formatter.format(date)));
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
            messageService.onEmbedMessage(event.getChannel(), messageCode, mentioned.getEffectiveName(),
                    result.getNumber(), argumentBuilder.toString());
            return;
        }
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.message",
                mentioned.getEffectiveName(), result.getNumber(), argumentBuilder.toString());
    }

    private List<Role> getRoles(Guild guild, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return roleIds.stream()
                .map(guild::getRoleById)
                .filter(e -> guild.getSelfMember().canInteract(e))
                .collect(Collectors.toList());
    }
}
