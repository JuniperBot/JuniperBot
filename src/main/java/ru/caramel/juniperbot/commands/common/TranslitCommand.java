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
package ru.caramel.juniperbot.commands.common;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.Command;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.CommonUtils;

@DiscordCommand(key = "discord.command.translit.key", description = "discord.command.translit.desc", priority = 4)
public class TranslitCommand implements Command {

    @Autowired
    private MessageService messageService;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        if (StringUtils.isEmpty(query)) {
            messageService.onTitledMessage(message.getChannel(), "discord.command.translit.title", "discord.command.translit.empty");
            return false;
        }
        String userName = message.getChannelType().isGuild()
                ? message.getMember().getEffectiveName()
                : message.getAuthor().getName();
        messageService.sendMessageSilent(message.getChannel()::sendMessage, messageService.getMessage(
                "discord.command.translit.format", userName, CommonUtils.untranslit(query)));
        return true;
    }
}
