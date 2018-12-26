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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.messaging.placeholder.GuildPlaceholderResolver;
import ru.caramel.juniperbot.core.messaging.placeholder.MessageTemplatePlaceholderResolver;
import ru.caramel.juniperbot.core.model.MessageTemplateCompiler;
import ru.caramel.juniperbot.core.model.enums.MessageTemplateType;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplate;
import ru.caramel.juniperbot.core.persistence.entity.MessageTemplateField;
import ru.caramel.juniperbot.core.persistence.repository.MessageTemplateRepository;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.service.MessageTemplateService;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.utils.DiscordUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private static PropertyPlaceholderHelper PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private MessageTemplateRepository repository;

    public Message compile(@NonNull MessageTemplateCompiler compiler) {
        try {
            String content = getContent(compiler);

            MessageTemplatePlaceholderResolver resolver = getPropertyResolver(compiler);

            if (compiler.getTemplate() == null || compiler.getTemplate().getType() == MessageTemplateType.TEXT) {
                content = processField(content, resolver, compiler, Message.MAX_CONTENT_LENGTH);
                if (StringUtils.isBlank(content)) {
                    return null;
                }
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.setContent(content);
                if (compiler.getTemplate() != null) {
                    messageBuilder.setTTS(compiler.getTemplate().isTts());
                }
                return messageBuilder.build();
            }

            MessageTemplate template = compiler.getTemplate();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setDescription(processField(content, resolver, compiler, MessageEmbed.TEXT_MAX_LENGTH));
            embedBuilder.setThumbnail(processField(template.getThumbnailUrl()));
            embedBuilder.setImage(processField(template.getImageUrl()));
            if (StringUtils.isNotEmpty(template.getColor())) {
                embedBuilder.setColor(Color.decode(template.getColor()));
            } else {
                embedBuilder.setColor(contextService.getDefaultColor());
            }

            embedBuilder.setAuthor(
                    processField(template.getAuthor(), resolver, compiler, MessageEmbed.TITLE_MAX_LENGTH),
                    processField(template.getAuthorUrl()),
                    processField(template.getAuthorIconUrl()));

            embedBuilder.setTitle(
                    processField(template.getTitle(), resolver, compiler, MessageEmbed.TITLE_MAX_LENGTH),
                    processField(template.getAuthorUrl()));

            embedBuilder.setFooter(
                    processField(template.getFooter(), resolver, compiler, MessageEmbed.TEXT_MAX_LENGTH),
                    processField(template.getFooterIconUrl()));

            int length = embedBuilder.length();

            if (CollectionUtils.isNotEmpty(template.getFields())) {
                for (MessageTemplateField templateField : template.getFields()) {
                    String name = processField(templateField.getName(), resolver, compiler, MessageEmbed.TITLE_MAX_LENGTH);
                    if (StringUtils.isEmpty(name)) {
                        name = "";
                    }
                    String value = processField(templateField.getValue(), resolver, compiler, MessageEmbed.VALUE_MAX_LENGTH);
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

    private void compileAndSend(@NonNull MessageTemplateCompiler compiler) {
        MessageTemplate template = compiler.getTemplate();
        Member member = compiler.getMember();
        Guild guild = compiler.getGuild();
        TextChannel fallbackChannel = compiler.getFallbackChannel();
        if ((template == null || StringUtils.isEmpty(template.getChannelId()))
                && fallbackChannel == null) {
            return;
        }

        if (template != null && DM_CHANNEL.equals(template.getChannelId())) {
            if (!compiler.isDirectAllowed() || member == null) {
                return;
            }

            try {
                contextService.queue(guild, member.getUser().openPrivateChannel(), channel -> {
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

        if (guild == null) {
            return;
        }

        contextService.withContext(guild, () -> {
            TextChannel channel = null;
            if (template != null && StringUtils.isNotEmpty(template.getChannelId())) {
                channel = guild.getTextChannelById(template.getChannelId());
            }

            if (!canTalk(channel) && compiler.getFallbackChannel() != null) {
                channel = fallbackChannel;
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

    private String getContent(@NonNull MessageTemplateCompiler compiler) {
        MessageTemplate template = compiler.getTemplate();
        String content = template != null ? template.getContent() : null;
        if (StringUtils.isNotEmpty(content)) {
            return content;
        }
        if (StringUtils.isEmpty(compiler.getFallbackContent())) {
            return null;
        }

        if (compiler.getGuild() != null) {
            return contextService.withContext(compiler.getGuild(),
                    () -> messageService.getMessage(compiler.getFallbackContent()));
        }
        return messageService.getMessage(compiler.getFallbackContent());
    }

    private MessageTemplatePlaceholderResolver getPropertyResolver(@NonNull MessageTemplateCompiler compiler) {
        Set<PropertyPlaceholderHelper.PlaceholderResolver> resolvers = new HashSet<>();
        resolvers.add(compiler.getVariables());
        if (compiler.getGuild() != null) {
            resolvers.add(GuildPlaceholderResolver.of(compiler.getGuild(), "guild"));
        }
        // TODO add member resolver
        return new MessageTemplatePlaceholderResolver(resolvers);
    }

    private static String processField(String value,
                                       MessageTemplatePlaceholderResolver resolver,
                                       MessageTemplateCompiler compiler,
                                       Integer maxLength) {
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

        if (compiler != null && compiler.getGuild() != null) {
            value = DiscordUtils.replaceReferences(value, compiler.getGuild());
        }

        if (maxLength != null && value.length() > maxLength) {
            value = CommonUtils.trimTo(value, maxLength);
        }
        return value;
    }

    private static String processField(String value) {
        return processField(value, null, null, null);
    }

    @Override
    public MessageTemplate getById(long id) {
        return repository.findById(id).orElse(null);
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
