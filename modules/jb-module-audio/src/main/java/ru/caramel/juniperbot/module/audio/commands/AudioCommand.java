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
package ru.caramel.juniperbot.module.audio.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.command.model.AbstractCommand;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.model.exception.ValidationException;
import ru.caramel.juniperbot.module.audio.service.LavaAudioService;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.module.audio.service.helper.AudioMessageManager;

public abstract class AudioCommand extends AbstractCommand {

    @Autowired
    protected PlayerService playerService;

    @Autowired
    protected MusicConfigService musicConfigService;

    @Autowired
    protected LavaAudioService audioService;

    @Autowired
    protected AudioMessageManager messageManager;

    protected abstract boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        Member member = message.getMember();
        if (member == null) {
            return false;
        }
        if (!featureSetService.isAvailable(message.getGuild())) {
            discordService.sendBonusMessage(message.getChannel().getIdLong());
            return false;
        }
        if (!musicConfigService.hasAccess(member)) {
            throw new ValidationException("discord.command.audio.missingAccess");
        }
        if (isActiveOnly() && !playerService.isActive(message.getGuild())) {
            messageManager.onMessage(message.getChannel(), "discord.command.audio.notStarted");
            return false;
        }
        if (isChannelRestricted() && !playerService.isInChannel(member)) {
            VoiceChannel channel = playerService.getChannel(member);
            throw new ValidationException("discord.command.audio.joinChannel", channel != null ? channel.getName() : "unknown");
        }
        return doInternal(message, context, content);
    }

    protected boolean isChannelRestricted() {
        return true;
    }

    protected boolean isActiveOnly() {
        return true;
    }
}
