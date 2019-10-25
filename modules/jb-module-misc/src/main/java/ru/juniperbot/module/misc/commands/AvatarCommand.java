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
package ru.juniperbot.module.misc.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.command.model.MentionableCommand;

@DiscordCommand(key = "discord.command.avatar.key",
        description = "discord.command.avatar.desc",
        group = "discord.command.group.utility",
        priority = 21)
public class AvatarCommand extends MentionableCommand {

    public AvatarCommand() {
        super(true, true);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        String avatarUrl = reference.getEffectiveAvatarUrl();

        if (StringUtils.isEmpty(avatarUrl)) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.avatar.none");
            return false;
        }
        String name = reference.getEffectiveName();

        EmbedBuilder builder = messageService.getBaseEmbed();
        if (StringUtils.isNotEmpty(name)) {
            builder.setDescription(messageService.getMessage("discord.command.avatar.text", name));
        }
        builder.setImage(avatarUrl + "?size=512");
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
        return true;
    }
}
