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
package ru.juniperbot.worker.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.juniperbot.common.configuration.RabbitConfiguration;
import ru.juniperbot.common.model.command.CommandInfo;
import ru.juniperbot.common.utils.LocaleUtils;
import ru.juniperbot.worker.common.command.model.Command;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.worker.common.command.service.CommandsHolderService;
import ru.juniperbot.worker.common.message.service.MessageService;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableRabbit
public class CommandListListener extends BaseQueueListener {

    @Autowired
    private CommandsHolderService holderService;

    @Autowired
    private MessageService messageService;

    @RabbitListener(queues = RabbitConfiguration.QUEUE_COMMAND_LIST_REQUEST)
    public List<CommandInfo> updateRanking(String dummy) {
        return holderService.getCommands()
                .values()
                .stream()
                .map(this::getInfo)
                .collect(Collectors.toList());
    }

    private CommandInfo getInfo(Command command) {
        DiscordCommand annotation = command.getAnnotation();
        CommandInfo.Builder builder = CommandInfo.builder()
                .key(annotation.key())
                .description(annotation.description())
                .group(annotation.group())
                .permissions(annotation.permissions())
                .priority(annotation.priority())
                .hidden(annotation.hidden());

        Map<String, String> keyLocalized = new HashMap<>();
        Map<String, String> descriptionLocalized = new HashMap<>();
        LocaleUtils.SUPPORTED_LOCALES.forEach((tag, locale) -> {
            keyLocalized.put(tag, messageService.getMessageByLocale(annotation.key(), locale));
            descriptionLocalized.put(tag, messageService.getMessageByLocale(annotation.description(), locale));
        });

        return builder
                .keyLocalized(keyLocalized)
                .descriptionLocalized(descriptionLocalized)
                .build();
    }
}
