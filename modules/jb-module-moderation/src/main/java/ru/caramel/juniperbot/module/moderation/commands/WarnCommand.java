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
package ru.caramel.juniperbot.module.moderation.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;

import java.util.Objects;

@DiscordCommand(key = "discord.command.mod.warn.key",
        description = "discord.command.mod.warn.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MESSAGE_WRITE, Permission.BAN_MEMBERS},
        priority = 10)
public class WarnCommand extends ModeratorCommandAsync {

    @Override
    public void doCommandAsync(MessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (mentioned == null) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.help",
                    context.getConfig().getPrefix());
            return;
        }
        if (moderationService.isModerator(mentioned) || Objects.equals(mentioned, event.getMember())) {
            fail(event); // do not allow ban members or yourself
            return;
        }
        if (moderationService.warn(event.getMember(), mentioned, removeMention(query))) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.ban.message", mentioned.getEffectiveName());
            return;
        }
        ModerationConfig config = moderationService.getConfig(event.getGuild());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warn.message",
                mentioned.getEffectiveName(), moderationService.warnCount(mentioned), config.getMaxWarnings());
    }
}
