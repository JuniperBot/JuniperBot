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
package ru.caramel.juniperbot.module.ranking.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.command.model.AbstractCommand;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.common.model.exception.DiscordException;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.module.ranking.service.RankingService;

public abstract class RankingCommand extends AbstractCommand {

    @Autowired
    protected RankingService rankingService;

    protected abstract boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        RankingConfig rankingConfig = rankingService.get(message.getGuild());
        return rankingConfig != null
                && rankingConfig.isEnabled()
                && doInternal(message, context, content);
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return guild != null && rankingService.isEnabled(guild.getIdLong());
    }
}
