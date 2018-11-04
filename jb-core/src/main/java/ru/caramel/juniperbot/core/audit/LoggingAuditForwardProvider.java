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
package ru.caramel.juniperbot.core.audit;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.model.ForwardProvider;
import ru.caramel.juniperbot.core.persistence.entity.AuditAction;
import ru.caramel.juniperbot.core.persistence.entity.AuditConfig;
import ru.caramel.juniperbot.core.persistence.entity.base.NamedReference;
import ru.caramel.juniperbot.core.persistence.repository.AuditConfigRepository;
import ru.caramel.juniperbot.core.service.AuditService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;

import java.time.Instant;

public abstract class LoggingAuditForwardProvider implements AuditForwardProvider {

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected AuditService auditService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected AuditConfigRepository configRepository;

    @Override
    @Transactional
    public void send(AuditConfig config, AuditAction action) {
        Class<?> clazz = this.getClass();
        if (config.getForwardChannelId() == null
                || !config.isForwardEnabled()
                || !config.getForwardActions().contains(action.getActionType())
                || !clazz.isAnnotationPresent(ForwardProvider.class)
                || clazz.getAnnotation(ForwardProvider.class).value() != action.getActionType()) {
            return;
        }
        if (!discordService.isConnected(action.getGuildId())) {
            return;
        }
        Guild guild = discordService.getGuildById(action.getGuildId());
        if (guild == null) {
            return;
        }
        TextChannel channel = guild.getTextChannelById(config.getForwardChannelId());
        if (channel == null || !guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        contextService.withContext(guild, () -> {
            EmbedBuilder embedBuilder = messageService.getBaseEmbed();
            MessageBuilder messageBuilder = new MessageBuilder();
            build(action, messageBuilder, embedBuilder);
            if (!embedBuilder.isEmpty()) {
                embedBuilder.setTimestamp(Instant.now());
                if (action.getActionType().getColor() != null) {
                    embedBuilder.setColor(action.getActionType().getColor());
                }
                messageBuilder.setEmbed(embedBuilder.build());
            }
            if (!messageBuilder.isEmpty()) {
                channel.sendMessage(messageBuilder.build()).queue();
            }
        });
    }

    protected String getReferenceContent(NamedReference reference, boolean channel) {
        return messageService.getMessage("audit.reference.content",
                reference.getName(),
                channel ? reference.getAsChannelMention() : reference.getAsUserMention());
    }

    protected String getReferenceShortContent(NamedReference reference) {
        return messageService.getMessage("audit.reference.short.content", reference.getName());
    }

    protected abstract void build(AuditAction action, MessageBuilder messageBuilder, EmbedBuilder embedBuilder);
}
