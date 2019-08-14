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
package ru.caramel.juniperbot.module.misc.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.command.model.AbstractCommand;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;

@DiscordCommand(key = "discord.command.avatar.key",
        description = "discord.command.avatar.desc",
        group = "discord.command.group.utility",
        priority = 21)
public class AvatarCommand extends AbstractCommand {

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        User user = message.getAuthor();
        String name = user.getName();
        if (CollectionUtils.isNotEmpty(message.getMessage().getMentionedMembers())) {
            Member member = getMentioned(message);
            user = member.getUser();
            name = member.getEffectiveName();
        }
        if (StringUtils.isEmpty(user.getAvatarUrl())) {
            messageService.onEmbedMessage(message.getChannel(), "discord.command.avatar.none");
            return false;
        }
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setDescription(messageService.getMessage("discord.command.avatar.text", name));
        builder.setImage(user.getAvatarUrl());
        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }
}
