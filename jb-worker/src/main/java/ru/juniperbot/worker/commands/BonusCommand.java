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
package ru.juniperbot.worker.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.FeatureSet;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.patreon.service.PatreonService;

@DiscordCommand(key = "discord.command.bonus.key",
        description = "discord.command.bonus.desc",
        group = "discord.command.group.utility",
        hidden = true,
        priority = 0)
public class BonusCommand extends AbstractCommand {

    @Autowired
    private PatreonService patreonService;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) {
        if ("-".equals(content)) {
            if (patreonService.removeBoost(message.getAuthor().getIdLong(), message.getGuild().getIdLong())) {
                messageService.onEmbedMessage(message.getChannel(), "discord.command.bonus.disabled");
            } else {
                messageService.onEmbedMessage(message.getChannel(), "discord.command.bonus.not-applied");
            }
        } else if (patreonService.tryBoost(message.getAuthor().getIdLong(), message.getGuild().getIdLong())) {
            String bonusCommand = messageService.getMessageByLocale("discord.command.bonus.key",
                    context.getCommandLocale());
            messageService.onEmbedMessage(message.getChannel(), "discord.command.bonus.applied",
                    context.getConfig().getPrefix(), bonusCommand);
        }
        return true;
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return patreonService.isAvailableForUser(user.getIdLong(), FeatureSet.BONUS);
    }
}
