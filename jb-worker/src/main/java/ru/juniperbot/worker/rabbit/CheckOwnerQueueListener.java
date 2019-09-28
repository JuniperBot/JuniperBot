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
package ru.juniperbot.worker.rabbit;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.request.CheckOwnerRequest;

@EnableRabbit
@Component
@Slf4j
public class CheckOwnerQueueListener extends BaseQueueListener {

    @RabbitListener(queues = RabbitConfiguration.QUEUE_CHECK_OWNER_REQUEST)
    public boolean isAdministrator(CheckOwnerRequest request) {
        try {
            Guild guild = null;
            switch (request.getType()) {
                case TEXT:
                    TextChannel textChannel = discordService.getTextChannelById(request.getChannelId());
                    if (textChannel != null) {
                        guild = textChannel.getGuild();
                    }
                    break;
                case VOICE:
                    VoiceChannel voiceChannel = discordService.getVoiceChannelById(request.getChannelId());
                    if (voiceChannel != null) {
                        guild = voiceChannel.getGuild();
                    }
                    break;
            }
            if (guild == null) {
                return true;
            }
            Member member = guild.getMemberById(request.getUserId());
            if (member == null) {
                return false;
            }
            return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR);
        } catch (Throwable e) {
            log.error("Could not detect administrator state", e);
            return false;
        }
    }
}
