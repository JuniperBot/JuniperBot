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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.juniperbot.common.model.CommandType;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.entity.CustomCommand;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.persistence.repository.CustomCommandRepository;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.message.model.MessageTemplateCompiler;
import ru.juniperbot.common.worker.message.service.MessageTemplateService;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();
        CustomCommand command = commandRepository.findByKeyAndGuildId(key, guild.getIdLong());
        if (command == null) {
            return false;
        }

        if (command.getCommandConfig() != null) {
            CommandConfig commandConfig = command.getCommandConfig();
            if (commandConfig.isDisabled() || isRestricted(event, commandConfig)) {
                return true;
            }
            if (commandConfig.isDeleteSource()
                    && self.hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                messageService.delete(event.getMessage());
            }
        }

        if (!moderationService.isModerator(event.getMember())) {
            content = DiscordUtils.maskPublicMentions(content);
        }

        if (command.getType() == CommandType.CHANGE_ROLES) {
            contextService.queue(guild, event.getChannel().sendTyping(), e -> changeRoles(event, command));
            return true;
        }

        MessageTemplateCompiler templateCompiler = templateService
                .createMessage(command.getMessageTemplate())
                .withGuild(guild)
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

    private void changeRoles(GuildMessageReceivedEvent event, CustomCommand command) {
        Guild guild = event.getGuild();
        Member self = guild.getSelfMember();
        Member targetMember = event.getMember();
        if (targetMember == null) {
            return;
        }

        if (!self.hasPermission(Permission.MANAGE_ROLES)) {
            String title = messageService.getMessage("discord.command.insufficient.permissions");
            String message = messageService.getMessage(messageService.getEnumTitle(Permission.MANAGE_ROLES));
            String changeTitle = messageService.getMessage("custom.roles.change.title", targetMember.getEffectiveName());
            messageService.onError(event.getChannel(), changeTitle, title + "\n\n" + message);
            return;
        }
        List<Role> currentRoles = targetMember.getRoles();
        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(command.getRolesToAdd())) {
            guild.getRoles().stream()
                    .filter(e -> command.getRolesToAdd().contains(e.getIdLong()))
                    .filter(e -> !currentRoles.contains(e))
                    .filter(e -> guild.getSelfMember().canInteract(e))
                    .forEach(rolesToAdd::add);
        }
        if (CollectionUtils.isNotEmpty(command.getRolesToRemove())) {
            guild.getRoles().stream()
                    .filter(e -> command.getRolesToRemove().contains(e.getIdLong()))
                    .filter(currentRoles::contains)
                    .filter(e -> guild.getSelfMember().canInteract(e))
                    .forEach(rolesToRemove::add);
        }

        String changeTitle = messageService.getMessage("custom.roles.change.title", targetMember.getEffectiveName());
        EmbedBuilder embedBuilder = messageService.getBaseEmbed()
                .setTitle(changeTitle, null);
        if (CollectionUtils.isEmpty(rolesToAdd) && CollectionUtils.isEmpty(rolesToRemove)) {
            embedBuilder.setDescription(messageService.getMessage("custom.roles.change.empty"));
            messageService.sendTempMessageSilent(event.getChannel()::sendMessage, embedBuilder.build(), 10);
            return;
        }

        contextService.queue(guild, guild.modifyMemberRoles(targetMember, rolesToAdd, rolesToRemove), e -> {
            if (CollectionUtils.isNotEmpty(rolesToAdd)) {
                embedBuilder.addField(messageService.getMessage("custom.roles.change.added"),
                        rolesToAdd.stream().map(Role::getAsMention).collect(Collectors.joining(", ")), false);
            }
            if (CollectionUtils.isNotEmpty(rolesToRemove)) {
                embedBuilder.addField(messageService.getMessage("custom.roles.change.removed"),
                        rolesToRemove.stream().map(Role::getAsMention).collect(Collectors.joining(", ")), false);
            }
            messageService.sendTempMessageSilent(event.getChannel()::sendMessage, embedBuilder.build(), 10);
        });
    }

    @Override
    public boolean isValidKey(GuildMessageReceivedEvent event, String key) {
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
