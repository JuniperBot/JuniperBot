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
package ru.juniperbot.worker.common.shared.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.juniperbot.worker.common.message.service.MessageService;
import ru.juniperbot.common.utils.CommonUtils;

import java.awt.*;

@Service
public class EmergencyServiceImpl implements EmergencyService {

    @Value("${discord.emergencyChannelId:}")
    private String emergencyChannelId;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageService messageService;

    @Override
    public void error(String message, Throwable throwable) {
        if (emergencyChannelId == null || !discordService.isConnected()) {
            return;
        }
        TextChannel channel = discordService.getShardManager().getTextChannelById(emergencyChannelId);
        if (channel == null) {
            return;
        }

        String errorText = String.format("`%s`\n\nStack trace:```javascript\n%s", throwable.getMessage(), ExceptionUtils.getStackTrace(throwable));
        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(message);
        builder.setColor(Color.RED);
        builder.setDescription(CommonUtils.trimTo(errorText, 2045) + "```");

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setContent("@here");
        messageBuilder.setEmbed(builder.build());
        messageService.sendMessageSilent(channel::sendMessage, messageBuilder.build());
    }

    @Override
    public void error(String message) {
        if (emergencyChannelId == null || !discordService.isConnected()) {
            return;
        }
        TextChannel channel = discordService.getShardManager().getTextChannelById(emergencyChannelId);
        if (channel == null) {
            return;
        }
        messageService.sendMessageSilent(channel::sendMessage, "@here " + message);
    }
}
