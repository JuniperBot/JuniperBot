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
package ru.juniperbot.common.worker.message.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.juniperbot.common.model.MessageTemplateType;
import ru.juniperbot.common.persistence.entity.MessageTemplate;
import ru.juniperbot.common.persistence.entity.MessageTemplateField;
import ru.juniperbot.common.persistence.repository.MessageTemplateRepository;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.message.model.MessageTemplateCompiler;
import ru.juniperbot.common.worker.message.resolver.ChannelPlaceholderResolver;
import ru.juniperbot.common.worker.message.resolver.GuildPlaceholderResolver;
import ru.juniperbot.common.worker.message.resolver.MemberPlaceholderResolver;
import ru.juniperbot.common.worker.message.resolver.MessageTemplatePlaceholderResolver;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.dv8tion.jda.api.EmbedBuilder.URL_PATTERN;

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

    @Autowired
    private ApplicationContext applicationContext;

    public Message compile(@NonNull MessageTemplateCompiler compiler) {
        try {
            String content = getContent(compiler);

            MessageTemplatePlaceholderResolver resolver = getPropertyResolver(compiler);

            if (compiler.getTemplate() == null || compiler.getTemplate().getType() == MessageTemplateType.TEXT) {
                content = processContent(content, resolver, compiler, Message.MAX_CONTENT_LENGTH, false);
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
            embedBuilder.setDescription(processContent(content, resolver, compiler, MessageEmbed.TEXT_MAX_LENGTH, false));
            embedBuilder.setThumbnail(processUrl(template.getThumbnailUrl(), resolver, compiler));
            embedBuilder.setImage(processUrl(template.getImageUrl(), resolver, compiler));
            if (StringUtils.isNotEmpty(template.getColor())) {
                embedBuilder.setColor(Color.decode(template.getColor()));
            } else {
                embedBuilder.setColor(contextService.getDefaultColor());
            }
            if (template.getTimestamp() != null) {
                embedBuilder.setTimestamp(template.getTimestamp().toInstant());
            }

            embedBuilder.setAuthor(
                    processContent(template.getAuthor(), resolver, compiler, MessageEmbed.TITLE_MAX_LENGTH, false),
                    processUrl(template.getAuthorUrl(), resolver, compiler),
                    processUrl(template.getAuthorIconUrl(), resolver, compiler));

            embedBuilder.setTitle(
                    processContent(template.getTitle(), resolver, compiler, MessageEmbed.TITLE_MAX_LENGTH, false),
                    processUrl(template.getTitleUrl(), resolver, compiler));

            embedBuilder.setFooter(
                    processContent(template.getFooter(), resolver, compiler, MessageEmbed.TEXT_MAX_LENGTH, false),
                    processUrl(template.getFooterIconUrl(), resolver, compiler));

            int length = embedBuilder.length();

            if (CollectionUtils.isNotEmpty(template.getFields())) {
                for (MessageTemplateField templateField : template.getFields()) {
                    String name = processContent(templateField.getName(), resolver, compiler,
                            MessageEmbed.TITLE_MAX_LENGTH, false);
                    if (StringUtils.isEmpty(name)) {
                        name = "";
                    }
                    String value = processContent(templateField.getValue(), resolver, compiler,
                            MessageEmbed.VALUE_MAX_LENGTH, false);
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
            log.error("Cannot compile message", e);
        }
        return null;
    }

    private TextChannel getTargetChannel(@NonNull MessageTemplateCompiler compiler) {
        MessageTemplate template = compiler.getTemplate();
        Guild guild = compiler.getGuild();
        if (guild == null || template != null && DM_CHANNEL.equals(template.getChannelId())) {
            return null;
        }

        TextChannel channel = null;
        if (template != null && StringUtils.isNotEmpty(template.getChannelId())) {
            channel = guild.getTextChannelById(template.getChannelId());
        }

        if (canTalk(channel)) {
            return channel;
        }

        channel = compiler.getFallbackChannel();
        return canTalk(channel) ? channel : null;
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

        TextChannel channel = getTargetChannel(compiler);
        if (channel != null) {
            contextService.withContext(channel.getGuild(), () -> {
                Message message = compile(compiler);
                if (message != null) {
                    messageService.sendMessageSilent(channel::sendMessage, message);
                }
            });
        }
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
        Locale locale = null;
        if (compiler.getGuild() != null) {
            locale = contextService.getLocale(compiler.getGuild());
            resolvers.add(GuildPlaceholderResolver.of(compiler.getGuild(), locale, applicationContext, "server"));
        }
        if (compiler.getMember() != null) {
            if (locale == null) {
                locale = contextService.getLocale(compiler.getMember().getGuild());
            }
            resolvers.add(MemberPlaceholderResolver.of(compiler.getMember(), locale, applicationContext, "member"));
        }
        TextChannel channel = getTargetChannel(compiler);
        if (channel != null) {
            if (locale == null) {
                locale = contextService.getLocale(channel.getGuild());
            }
            resolvers.add(ChannelPlaceholderResolver.of(channel, locale, applicationContext, "channel"));
        }
        return new MessageTemplatePlaceholderResolver(resolvers);
    }

    private static String processContent(String value,
                                         MessageTemplatePlaceholderResolver resolver,
                                         MessageTemplateCompiler compiler,
                                         Integer maxLength,
                                         boolean placeholdersOnly) {
        value = value != null ? value.trim() : null;
        if (StringUtils.isBlank(value)) {
            return null;
        }

        if (!placeholdersOnly && compiler != null && compiler.getGuild() != null) {
            value = DiscordUtils.replaceReferences(value, compiler.getGuild());
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

    private static String processUrl(String url,
                                     MessageTemplatePlaceholderResolver resolver,
                                     MessageTemplateCompiler compiler) {
        url = processContent(url, resolver, compiler, MessageEmbed.URL_MAX_LENGTH, true);
        return StringUtils.isNotBlank(url) && URL_PATTERN.matcher(url).matches() ? url : null;
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

            @Override
            public String processContent(String content, boolean placeholdersOnly) {
                MessageTemplatePlaceholderResolver resolver = getPropertyResolver(this);
                return MessageTemplateServiceImpl.processContent(content, resolver, this, null, placeholdersOnly);
            }
        };
    }
}
