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
package ru.caramel.juniperbot.core.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplate;

/**
 * Handles message templates
 */
public interface MessageTemplateService {

    String DM_CHANNEL = "-1";

    /**
     * Compiles template exposing variables and builds it to JDA Message instance
     *
     * @param template Message template to compile
     * @param guild    Related guild instance
     * @return The compiled message instance
     * @see Message
     */
    Message compile(MessageTemplate template, Guild guild);

    /**
     * Compiles and sends message if channel specified in template
     *
     * @param template Message template to compile and send
     * @param guild    Related guild instance
     * @see MessageTemplate#channelId
     */
    void compileAndSend(MessageTemplate template, Guild guild);
}
