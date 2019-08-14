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
package ru.caramel.juniperbot.module.audio.commands.timing;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.audio.commands.AudioCommand;
import ru.caramel.juniperbot.module.audio.model.TrackRequest;

public abstract class TimingCommand extends AudioCommand {

    protected abstract boolean doInternal(GuildMessageReceivedEvent message, TrackRequest request, long millis);

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        TrackRequest current = playerService.get(message.getGuild()).getCurrent();
        if (current == null) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.notStarted");
            return false;
        }
        if (!current.getTrack().isSeekable()) {
            messageManager.onQueueError(message.getChannel(), "discord.command.audio.seek.notApplicable");
            return false;
        }

        Long millis = CommonUtils.parseMillis(content);
        if (millis == null) {
            messageManager.onQueueError(message.getChannel(), "discord.command.audio.seek.format");
            return false;
        }
        return doInternal(message, current, millis);
    }
}
