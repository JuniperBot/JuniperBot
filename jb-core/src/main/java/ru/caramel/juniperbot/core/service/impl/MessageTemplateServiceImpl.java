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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.messaging.MessageTemplatePropertyResolver;
import ru.caramel.juniperbot.core.model.MessageTemplateCompiler;
import ru.caramel.juniperbot.core.model.enums.MessageTemplateType;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplate;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplateField;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.DiscordService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.service.MessageTemplateService;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private static final Logger log = LoggerFactory.getLogger(MessageTemplateServiceImpl.class);

    private static PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DiscordService discordService;

    public Message compile(MessageTemplateCompiler compiler) {
        try {
            String content = getContent(compiler);

            MessageTemplatePropertyResolver resolver = getPropertyResolver(compiler);

            if (compiler.getTemplate() == null || compiler.getTemplate().getType() == MessageTemplateType.TEXT) {
                content = processField(content, resolver, Message.MAX_CONTENT_LENGTH);
                if (StringUtils.isBlank(content)) {
                    return null;
                }
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.setContent(content);
                return messageBuilder.build();
            }

            MessageTemplate template = compiler.getTemplate();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setDescription(processField(content, resolver, MessageEmbed.TEXT_MAX_LENGTH));
            embedBuilder.setThumbnail(processField(template.getThumbnailUrl()));
            embedBuilder.setImage(processField(template.getImageUrl()));
            if (StringUtils.isNotEmpty(template.getColor())) {
                embedBuilder.setColor(Color.decode(template.getColor()));
            } else {
                embedBuilder.setColor(contextService.getDefaultColor());
            }

            embedBuilder.setAuthor(
                    processField(template.getAuthor(), resolver, MessageEmbed.TITLE_MAX_LENGTH),
                    processField(template.getAuthorUrl()),
                    processField(template.getAuthorIconUrl()));

            embedBuilder.setTitle(
                    processField(template.getTitle(), resolver, MessageEmbed.TITLE_MAX_LENGTH),
                    processField(template.getAuthorUrl()));

            embedBuilder.setFooter(
                    processField(template.getFooter(), resolver, MessageEmbed.TEXT_MAX_LENGTH),
                    processField(template.getFooterIconUrl()));

            int length = embedBuilder.length();

            if (CollectionUtils.isNotEmpty(template.getFields())) {
                for (MessageTemplateField templateField : template.getFields()) {
                    String name = processField(templateField.getName(), resolver, MessageEmbed.TITLE_MAX_LENGTH);
                    if (StringUtils.isEmpty(name)) {
                        name = "";
                    }
                    String value = processField(templateField.getValue(), resolver, MessageEmbed.VALUE_MAX_LENGTH);
                    if (StringUtils.isEmpty(value)) {
                        value = "";
                    }

                    if ((length += name.length() + value.length()) > MessageEmbed.EMBED_MAX_LENGTH_BOT) {
                        break;
                    }
                    embedBuilder.addField(name, value, templateField.isInline());
                }
            }

            if (embedBuilder.isEmpty()) {
                return null;
            }
            MessageBuilder messageBuilder = new MessageBuilder();
            messageBuilder.setEmbed(embedBuilder.build());
            return messageBuilder.build();
        } catch (IllegalArgumentException e) {
            log.warn("Cannot compile message", e);
        }
        return null;
    }

    private void compileAndSend(MessageTemplateCompiler compiler) {
        MessageTemplate template = compiler.getTemplate();
        if ((template == null || StringUtils.isEmpty(template.getChannelId()))
                && compiler.getFallbackChannelId() == null) {
            return;
        }

        if (template != null && DM_CHANNEL.equals(template.getChannelId())) {
            if (!compiler.isDirectAllowed() || compiler.getMemberId() == null) {
                return;
            }
            User user = discordService.getUserById(compiler.getMemberId());
            if (user == null) {
                return;
            }

            try {
                contextService.queue(compiler.getGuildId(), user.openPrivateChannel(), channel -> {
                    Message message = compile(compiler);
                    if (message != null) {
                        messageService.sendMessageSilent(channel::sendMessage, message);
                    }
                });
            } catch (Exception e) {
                // fall down, we don't care
            }
            return;
        }

        if (compiler.getGuildId() == null) {
            return;
        }

        Guild guild = discordService.getGuildById(compiler.getGuildId());
        if (guild == null) {
            return;
        }

        contextService.withContext(guild, () -> {
            TextChannel channel = null;
            if (template != null && StringUtils.isNotEmpty(template.getChannelId())) {
                channel = guild.getTextChannelById(template.getChannelId());
            }

            if (!canTalk(channel) && compiler.getFallbackChannelId() != null) {
                channel = guild.getTextChannelById(compiler.getFallbackChannelId());
            }

            if (canTalk(channel)) {
                Message message = compile(compiler);
                if (message != null) {
                    messageService.sendMessageSilent(channel::sendMessage, message);
                }
            }
        });
    }

    private boolean canTalk(TextChannel channel) {
        return channel != null && channel.canTalk() &&
                channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS);
    }

    private String getContent(MessageTemplateCompiler compiler) {
        MessageTemplate template = compiler.getTemplate();
        String content = template != null ? template.getContent() : null;
        if (StringUtils.isNotEmpty(content)) {
            return content;
        }
        if (StringUtils.isEmpty(compiler.getFallbackContent())) {
            return null;
        }

        if (compiler.getGuildId() != null) {
            return contextService.withContext(compiler.getGuildId(),
                    () -> messageService.getMessage(compiler.getFallbackContent()));
        }
        return messageService.getMessage(compiler.getFallbackContent());
    }

    private MessageTemplatePropertyResolver getPropertyResolver(MessageTemplateCompiler compiler) {
        Set<PropertyPlaceholderHelper.PlaceholderResolver> resolvers = new HashSet<>();
        resolvers.add(compiler.getVariables());
        // to do add more
        return new MessageTemplatePropertyResolver(resolvers);
    }

    private static String processField(String value, MessageTemplatePropertyResolver resolver, Integer maxLength) {
        value = value != null ? value.trim() : null;
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (resolver != null) {
            value = PLACEHOLDER_HELPER.replacePlaceholders(value, resolver);
        }
        if (StringUtils.isBlank(value)) {
            return null; // check it second time
        }
        if (maxLength != null && value.length() > maxLength) {
            value = CommonUtils.trimTo(value, maxLength);
        }
        return value;
    }

    private static String processField(String value, Integer maxLength) {
        return processField(value, null, maxLength);
    }

    private static String processField(String value) {
        return processField(value, null, null);
    }

    @Override
    public MessageTemplateCompiler createMessage(MessageTemplate template) {
        return new MessageTemplateCompiler(template) {

            @Override
            public Message compile() {
                return MessageTemplateServiceImpl.this.compile(this);
            }

            @Override
            public void compileAndSend() {
                MessageTemplateServiceImpl.this.compileAndSend(this);
            }
        };
    }
}
