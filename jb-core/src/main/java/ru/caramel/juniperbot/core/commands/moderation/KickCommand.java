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
package ru.caramel.juniperbot.core.commands.moderation;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

import java.util.Objects;

@DiscordCommand(key = "discord.command.mod.kick.key",
        description = "discord.command.mod.kick.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.KICK_MEMBERS},
        priority = 20)
public class KickCommand extends ModeratorCommand {

    @Override
    public boolean doCommand(MessageReceivedEvent event, BotContext context, String query) {
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
        moderationService.kick(event.getMember(), mentioned, removeMention(query));
        return ok(event);
    }
}
