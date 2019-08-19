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
package ru.juniperbot.worker.commands.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.worker.common.moderation.model.ModerationActionRequest;
import ru.juniperbot.common.model.ModerationActionType;

import java.util.Objects;

@DiscordCommand(key = "discord.command.mod.kick.key",
        description = "discord.command.mod.kick.desc",
        group = "discord.command.group.moderation",
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.KICK_MEMBERS},
        priority = 20)
public class KickCommand extends ModeratorCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (mentioned == null) {
            String kickCommand = messageService.getMessageByLocale("discord.command.mod.kick.key",
                    context.getCommandLocale());
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.kick.help",
                    context.getConfig().getPrefix(), kickCommand);
            return false;
        }
        if (moderationService.isModerator(mentioned) || Objects.equals(mentioned, event.getMember())) {
            return fail(event); // do not allow kick members or yourself
        }
        if (!event.getGuild().getSelfMember().canInteract(mentioned)) {
            messageService.onError(event.getChannel(), "discord.command.mod.kick.position");
            return false;
        }

        ModerationActionRequest request = ModerationActionRequest.builder()
                .type(ModerationActionType.KICK)
                .moderator(event.getMember())
                .violator(mentioned)
                .reason(removeMention(query))
                .build();

        if (moderationService.performAction(request)) {
            return ok(event);
        }
        return fail(event);
    }
}
