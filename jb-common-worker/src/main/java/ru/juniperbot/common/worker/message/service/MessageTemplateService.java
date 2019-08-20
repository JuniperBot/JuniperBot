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
package ru.juniperbot.common.worker.message.service;

import ru.juniperbot.common.persistence.entity.MessageTemplate;
import ru.juniperbot.common.worker.message.model.MessageTemplateCompiler;

/**
 * Handles message templates
 */
public interface MessageTemplateService {

    String DM_CHANNEL = "-1";

    MessageTemplate getById(long id);

    /**
     * Creates a message compiler for specified template
     *
     * @param template Template for compiler
     * @return Compiler instance
     */
    MessageTemplateCompiler createMessage(MessageTemplate template);
}
