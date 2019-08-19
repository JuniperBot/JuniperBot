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
package ru.caramel.juniperbot.module.mafia.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;

@DiscordCommand(key = "discord.command.mafia.help.key",
        description = "discord.command.mafia.help.desc",
        group = "discord.command.group.mafia",
        priority = 15)
public class MafiaHelpCommand extends MafiaCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        EmbedBuilder builder = messageService.getBaseEmbed(true);
        builder.setDescription(messageService.getMessage("mafia.help.description") +
                "\n" + EmbedBuilder.ZERO_WIDTH_SPACE);

        builder.addField(getField("mafia.team.mafia", "mafia.help.team.mafia"));
        builder.addField(getField("mafia.team.innocent", "mafia.help.team.innocent"));
        builder.addField(getField("mafia.help.day.title", "mafia.help.day.content"));
        builder.addField(getField("mafia.help.order.title", "mafia.help.order.content"));

        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    public MessageEmbed.Field getField(String title, String content) {
        return new MessageEmbed.Field(messageService.getMessage(title) + ":",
                messageService.getMessage(content) + "\n" + EmbedBuilder.ZERO_WIDTH_SPACE, true);
    }
}
