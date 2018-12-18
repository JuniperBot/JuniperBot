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
package ru.caramel.juniperbot.core.service.impl;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplate;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.service.MessageTemplateService;

@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    @Autowired
    private MessageService messageService;

    @Override
    public Message compile(MessageTemplate template, Guild guild) {
        // TODO compile template and return builder
        return null;
    }

    @Override
    public void compileAndSend(MessageTemplate template, Guild guild) {
        if (StringUtils.isEmpty(template.getChannelId())) {
            return;
        }
        TextChannel channel = guild.getTextChannelById(template.getChannelId());
        if (channel == null) {
            return;
        }
        Message message = compile(template, guild);
        messageService.sendMessageSilent(channel::sendMessage, message);
    }
}
