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

import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    private Map<Class<?>, Map<String, Enum<?>>> enumCache = new ConcurrentHashMap<>();

    @Getter
    private Color accentColor;

    @Getter
    @Value("${message.copy.imageUrl}")
    private String copyImageUrl;

    @Getter
    @Value("${message.copy.content}")
    private String copyContent;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ContextService contextService;

    @Override
    public EmbedBuilder getBaseEmbed() {
        return new EmbedBuilder().setColor(accentColor);
    }

    @Override
    public void onError(MessageChannel sourceChannel, String code, Object... args) {
        onError(sourceChannel, "discord.message.title.error", code, args);
    }

    @Override
    public void onError(MessageChannel sourceChannel, String titleCode, String code, Object... args) {
        sendMessageSilent(sourceChannel::sendMessage, createMessage(titleCode, code, args)
                .setColor(Color.RED).build());
    }

    @Override
    public void onMessage(MessageChannel sourceChannel, String code, Object... args) {
        onTitledMessage(sourceChannel, null, code, args);
    }

    @Override
    public void onTitledMessage(MessageChannel sourceChannel, String titleCode, String code, Object... args) {
        if (StringUtils.isEmpty(titleCode)) {
            sendMessageSilent(sourceChannel::sendMessage, getMessage(code, args));
            return;
        }
        sendMessageSilent(sourceChannel::sendMessage, createMessage(titleCode, code, args).build());
    }

    private EmbedBuilder createMessage(String titleCode, String code, Object... args) {
        return getBaseEmbed()
                .setTitle(getMessage(titleCode), null)
                .setColor(Color.RED)
                .setDescription(getMessage(code, args));
    }

    @Override
    public String getMessage(String key, Object... args) {
        return getMessage(key, contextService.getLocale(), args);
    }

    @Override
    public String getMessage(String key, Locale locale, Object... args) {
        if (key == null) {
            return null;
        }
        return context.getMessage(key, args, key, locale);
    }

    @Override
    public boolean hasMessage(String code) {
        return StringUtils.isNotEmpty(code) &&
                context.getMessage(code, null, null,
                        contextService.getLocale()) != null;
    }

    @Override
    public <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed) {
        try {
            action.apply(embed).queue();
        } catch (PermissionException e) {
            LOGGER.warn("No permission to message", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> T getEnumeration(Class<T> clazz, String title) {
        Map<String, Enum<?>> cache = enumCache.computeIfAbsent(clazz, e ->
                Stream.of(clazz.getEnumConstants()).collect(Collectors.toMap(k -> getEnumTitle(k).toLowerCase(), v -> v)));
        return (T) cache.get(title.toLowerCase());
    }

    @Override
    public String getEnumTitle(Enum<?> value) {
        if (value == null) {
            return null;
        }
        return getMessage(value.getClass().getName() + "." + value.name());
    }

    @Value("${message.accentColor:#FFA550}")
    public void setAccentColor(String color) {
        accentColor = StringUtils.isNotEmpty(color) ? Color.decode(color) : null;
    }
}
