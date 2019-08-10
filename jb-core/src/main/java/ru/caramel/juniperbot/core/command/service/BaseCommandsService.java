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

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.command.model.CoolDownHolder;
import ru.caramel.juniperbot.core.command.model.CoolDownMode;
import ru.caramel.juniperbot.core.command.persistence.CommandConfig;
import ru.caramel.juniperbot.core.common.persistence.GuildConfig;
import ru.caramel.juniperbot.core.common.service.ConfigService;
import ru.caramel.juniperbot.core.event.service.ContextService;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.core.moderation.persistence.ModerationConfig;
import ru.caramel.juniperbot.core.moderation.service.ModerationService;

import java.util.Date;

@Slf4j
public abstract class BaseCommandsService implements CommandsService, CommandHandler {

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected ModerationService moderationService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected CommandsHolderService holderService;

    @Override
    @Transactional
    public boolean handleMessage(GuildMessageReceivedEvent event) {
        JDA jda = event.getJDA();
        String content = event.getMessage().getContentRaw().trim();
        if (StringUtils.isEmpty(content)) {
            return false;
        }

        String prefix = null;
        String input = content;
        boolean usingMention = false;

        if (event.getMessage().isMentioned(jda.getSelfUser())) {
            String mention = jda.getSelfUser().getAsMention();
            if (!(usingMention = content.startsWith(mention))) {
                mention = String.format("<@!%s>", jda.getSelfUser().getId());
                usingMention = content.startsWith(mention);
            }
            if (usingMention) {
                prefix = mention;
                input = content.substring(prefix.length()).trim();
            }
        }

        String firstPart = input.split("\\s+", 2)[0].trim();
        if (!isValidKey(event, firstPart)) {
            return false;
        }

        GuildConfig guildConfig = configService.getOrCreate(event.getGuild());

        if (!usingMention) {
            prefix = guildConfig != null ? guildConfig.getPrefix() : configService.getDefaultPrefix();
            if (prefix.length() > content.length()) {
                return true;
            }
            input = content.substring(prefix.length()).trim();
        }
        if (StringUtils.isNotEmpty(prefix) && content.toLowerCase().startsWith(prefix.toLowerCase())) {
            String[] args = input.split("\\s+", 2);
            input = args.length > 1 ? args[1] : "";
            return sendCommand(event, input, args[0], guildConfig);
        }
        return true;
    }

    @Override
    public void resultEmotion(GuildMessageReceivedEvent message, String emoji, String messageCode, Object... args) {
        try {
            if (message.getGuild() == null || message.getGuild().getSelfMember().hasPermission(message.getChannel(),
                    Permission.MESSAGE_ADD_REACTION)) {
                try {
                    message.getMessage().addReaction(emoji).queue();
                    return;
                } catch (Exception e) {
                    // fall down and add emoticon as message
                }
            }
            String text = emoji;
            if (StringUtils.isNotEmpty(messageCode) && messageService.hasMessage(messageCode)) {
                text = messageService.getMessage(messageCode, args);
            }
            messageService.sendMessageSilent(message.getChannel()::sendMessage, text);
        } catch (Exception e) {
            log.error("Add emotion error", e);
        }
    }

    @Override
    public boolean isRestricted(CommandConfig commandConfig, TextChannel channel) {
        if (channel == null || commandConfig == null) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(commandConfig.getAllowedChannels())
                && !commandConfig.getAllowedChannels().contains(channel.getIdLong())) {
            return true;
        }
        return CollectionUtils.isNotEmpty(commandConfig.getIgnoredChannels())
                && commandConfig.getIgnoredChannels().contains(channel.getIdLong());
    }

    @Override
    public boolean isRestricted(CommandConfig commandConfig, Member member) {
        if (member == null || commandConfig == null) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(commandConfig.getAllowedRoles())
                && member.getRoles().stream().noneMatch(e -> commandConfig.getAllowedRoles().contains(e.getIdLong()))) {
            return true;
        }
        return CollectionUtils.isNotEmpty(commandConfig.getIgnoredRoles())
                && member.getRoles().stream().anyMatch(e -> commandConfig.getIgnoredRoles().contains(e.getIdLong()));
    }

    @Override
    @Transactional
    public boolean isRestricted(GuildMessageReceivedEvent event, CommandConfig commandConfig) {
        if (isRestricted(commandConfig, event.getChannel())) {
            resultEmotion(event, "✋", null);
            messageService.onTempEmbedMessage(event.getChannel(), 10, "discord.command.restricted.channel");
            return true;
        }
        if (isRestricted(commandConfig, event.getMember())) {
            resultEmotion(event, "✋", null);
            messageService.onTempEmbedMessage(event.getChannel(), 10, "discord.command.restricted.roles");
            return true;
        }
        if (event.getMember() != null && commandConfig.getCoolDownMode() != CoolDownMode.NONE) {
            if (CollectionUtils.isEmpty(commandConfig.getCoolDownIgnoredRoles()) ||
                    event.getMember().getRoles().stream().noneMatch(e -> commandConfig.getCoolDownIgnoredRoles().contains(e.getIdLong()))) {
                ModerationConfig moderationConfig = moderationService.get(event.getGuild());
                if (!moderationService.isModerator(event.getMember())
                        || (moderationConfig != null && !moderationConfig.isCoolDownIgnored())) {
                    CoolDownHolder holder = holderService.getCoolDownHolderMap()
                            .computeIfAbsent(event.getGuild().getIdLong(), CoolDownHolder::new);
                    long duration = holder.perform(event, commandConfig);
                    if (duration > 0) {
                        resultEmotion(event, "\uD83D\uDD5C", null);
                        Date date = new Date();
                        date.setTime(date.getTime() + duration);

                        PrettyTime time = new PrettyTime(contextService.getLocale());
                        time.removeUnit(JustNow.class);
                        messageService.onTempEmbedMessage(event.getChannel(), 10,
                                "discord.command.restricted.cooldown", time.format(date));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
