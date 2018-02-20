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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

@DiscordCommand(key = "discord.command.mod.slow.key",
        description = "discord.command.mod.slow.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE},
        priority = 25)
public class SlowModeCommand extends ModeratorCommand {

    @Override
    public boolean doCommand(MessageReceivedEvent event, BotContext context, String query) {
        if (StringUtils.isNumeric(query)) {
            int seconds = Integer.parseInt(query);
            String secPlurals = messageService.getCountPlural(seconds, "discord.plurals.second");
            moderationService.slowMode(event.getTextChannel(), seconds);
            messageService.onEmbedMessage(event.getChannel(),"discord.command.mod.slow.enabled", seconds,
                    secPlurals);
            return true;
        } else if (messageService.getMessage("discord.command.mod.slow.off").equalsIgnoreCase(query)) {
            boolean disabled = moderationService.slowOff(event.getTextChannel());
            messageService.onEmbedMessage(event.getChannel(), disabled
                    ? "discord.command.mod.slow.disabled" : "discord.command.mod.slow.disabled.already");
            return true;
        }
        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.slow.help", context.getConfig().getPrefix());
        return false;
    }
}
