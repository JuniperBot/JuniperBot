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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.AbstractCommand;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.module.ranking.service.RankingService;

public abstract class RankingCommand extends AbstractCommand {

    @Autowired
    protected RankingService rankingService;

    @Autowired
    protected MessageService messageService;

    protected abstract boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        RankingConfig rankingConfig = rankingService.getConfig(message.getGuild());
        return rankingConfig.isEnabled()
                && !rankingService.isBanned(rankingConfig, message.getMember())
                && doInternal(message, context, content);
    }

    @Override
    public boolean isAvailable(GuildConfig config) {
        return rankingService.isEnabled(config.getGuildId());
    }
}
