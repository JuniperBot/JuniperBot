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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.worker.command.model.AbstractCommand;
import ru.juniperbot.common.worker.command.model.BotContext;

public abstract class RankingCommand extends AbstractCommand {

    @Autowired
    protected RankingConfigService rankingConfigService;

    protected abstract boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        RankingConfig rankingConfig = rankingConfigService.get(message.getGuild());
        return rankingConfig != null
                && rankingConfig.isEnabled()
                && doInternal(message, context, content);
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return guild != null && rankingConfigService.isEnabled(guild.getIdLong());
    }
}
