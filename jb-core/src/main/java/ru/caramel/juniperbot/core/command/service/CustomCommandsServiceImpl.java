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
package ru.caramel.juniperbot.core.command.service;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.caramel.juniperbot.core.command.persistence.CommandConfig;
import ru.caramel.juniperbot.core.command.persistence.CustomCommand;
import ru.caramel.juniperbot.core.command.persistence.CustomCommandRepository;
import ru.caramel.juniperbot.core.common.persistence.GuildConfig;
import ru.caramel.juniperbot.core.common.service.ConfigService;
import ru.caramel.juniperbot.core.message.model.MessageTemplateCompiler;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.core.message.service.MessageTemplateService;
import ru.caramel.juniperbot.core.moderation.service.ModerationService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.DiscordUtils;

import javax.annotation.PostConstruct;
import java.util.regex.Pattern;

@Service
public class CustomCommandsServiceImpl implements CustomCommandsService {

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageTemplateService templateService;

    @Autowired
    private CustomCommandRepository commandRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ModerationService moderationService;

    @PostConstruct
    public void init() {
        commandsService.registerHandler(this);
    }

    @Override
    public boolean handleMessage(GuildMessageReceivedEvent event) {
        return commandsService.sendMessage(event, this, e -> isAnyCustomCommand(event, e));
    }

    public boolean isAnyCustomCommand(GuildMessageReceivedEvent event, String input) {
        if (event.getGuild() == null) {
            return false;
        }
        String prefix = configService.getPrefix(event.getGuild().getIdLong());
        if (StringUtils.isEmpty(prefix)) {
            return false;
        }
        if (!input.startsWith(prefix)) {
            return false;
        }
        String key = input.replaceFirst("^" + Pattern.quote(prefix), "");
        return commandRepository.existsByKeyAndGuildId(key, event.getGuild().getIdLong());
    }

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
            if (commandConfig.isDisabled() || commandsService.isRestricted(event, commandConfig)) {
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
                    return commandsService.sendCommand(event, CommonUtils.trimTo(commandContent, 2000), args[0], config);
                }
                break;
            case MESSAGE:
                templateCompiler.compileAndSend();
                return true;
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
