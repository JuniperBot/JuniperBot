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
package ru.juniperbot.worker.common.command.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.service.ModerationConfigService;
import ru.juniperbot.worker.common.command.model.CoolDownHolder;
import ru.juniperbot.common.model.CoolDownMode;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.service.ConfigService;
import ru.juniperbot.worker.common.event.service.ContextService;
import ru.juniperbot.worker.common.message.service.MessageService;
import ru.juniperbot.common.persistence.entity.ModerationConfig;
import ru.juniperbot.worker.common.modules.moderation.service.ModerationService;
import ru.juniperbot.worker.common.shared.service.DiscordEntityAccessor;

import java.util.Date;

@Slf4j
public abstract class BaseCommandsService implements CommandsService, CommandHandler {

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    protected DiscordEntityAccessor entityAccessor;

    @Autowired
    protected ModerationService moderationService;

    @Autowired
    protected ModerationConfigService moderationConfigService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected CoolDownService coolDownService;

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

        GuildConfig guildConfig = entityAccessor.getOrCreate(event.getGuild());

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
                ModerationConfig moderationConfig = moderationConfigService.getByGuildId(event.getGuild().getIdLong());
                if (!moderationService.isModerator(event.getMember())
                        || (moderationConfig != null && !moderationConfig.isCoolDownIgnored())) {
                    CoolDownHolder holder = coolDownService.getCoolDownHolderMap()
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
