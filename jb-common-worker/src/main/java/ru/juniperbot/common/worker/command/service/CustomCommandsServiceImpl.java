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
package ru.juniperbot.common.worker.command.service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.entity.CustomCommand;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.repository.CustomCommandRepository;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.message.model.MessageTemplateCompiler;
import ru.juniperbot.common.worker.message.service.MessageTemplateService;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.util.regex.Pattern;

@Order(10)
@Service
public class CustomCommandsServiceImpl extends BaseCommandsService {

    @Autowired
    private InternalCommandsService internalCommandsService;

    @Autowired
    private MessageTemplateService templateService;

    @Autowired
    private CustomCommandRepository commandRepository;

    @Override
    public boolean sendCommand(GuildMessageReceivedEvent event, String content, String key, GuildConfig config) {
        if (event.getGuild() == null) {
            return false;
        }
        CustomCommand command = commandRepository.findByKeyAndGuildId(key, event.getGuild().getIdLong());
        if (command == null) {
            return false;
        }

        if (command.getCommandConfig() != null) {
            CommandConfig commandConfig = command.getCommandConfig();
            if (commandConfig.isDisabled() || isRestricted(event, commandConfig)) {
                return true;
            }
            if (commandConfig.isDeleteSource()
                    && event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                messageService.delete(event.getMessage());
            }
        }

        if (!moderationService.isModerator(event.getMember())) {
            content = DiscordUtils.maskPublicMentions(content);
        }

        MessageTemplateCompiler templateCompiler = templateService
                .createMessage(command.getMessageTemplate())
                .withGuild(event.getGuild())
                .withMember(event.getMember())
                .withFallbackChannel(event.getChannel())
                .withVariable("content", content);

        switch (command.getType()) {
            case ALIAS:
                String commandContent = templateCompiler.processContent(command.getContent(), true);
                String[] args = commandContent.split("\\s+");
                if (args.length > 0) {
                    commandContent = commandContent.substring(args[0].length()).trim();
                    return internalCommandsService.sendCommand(event, CommonUtils.trimTo(commandContent, 2000), args[0], config);
                }
                break;
            case MESSAGE:
                templateCompiler.compileAndSend();
                break;
        }
        return true;
    }

    @Override
    public boolean isValidKey(GuildMessageReceivedEvent event, String key) {
        if (event.getGuild() == null) {
            return false;
        }
        String prefix = configService.getPrefix(event.getGuild().getIdLong());
        if (StringUtils.isEmpty(prefix)) {
            return false;
        }
        if (!key.startsWith(prefix)) {
            return false;
        }
        key = key.replaceFirst("^" + Pattern.quote(prefix), "");
        return commandRepository.existsByKeyAndGuildId(key, event.getGuild().getIdLong());
    }
}
