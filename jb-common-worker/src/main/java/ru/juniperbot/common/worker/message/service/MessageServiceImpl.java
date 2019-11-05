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

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.configuration.CommonConfiguration;
import ru.juniperbot.common.configuration.CommonProperties;
import ru.juniperbot.common.utils.LocaleUtils;
import ru.juniperbot.common.utils.PluralUtils;
import ru.juniperbot.common.worker.event.service.ContextService;
import ru.juniperbot.common.worker.modules.audit.service.ActionsHolderService;
import ru.juniperbot.common.worker.utils.DiscordUtils;

import java.awt.*;
import java.time.Year;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    private Map</* enum class */Class<?>, Map</* locale */Locale, Map</* value */String, Enum<?>>>> enumCache = new ConcurrentHashMap<>();

    @Autowired
    private CommonProperties commonProperties;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ContextService contextService;

    @Autowired
    @Qualifier(CommonConfiguration.SCHEDULER)
    private TaskScheduler scheduler;

    @Autowired
    private ActionsHolderService actionsHolderService;

    @Override
    public EmbedBuilder getBaseEmbed() {
        return getBaseEmbed(false);
    }

    @Override
    public EmbedBuilder getBaseEmbed(boolean copyright) {
        EmbedBuilder builder = new EmbedBuilder().setColor(contextService.getColor());
        if (copyright) {
            builder.setFooter(getMessage("about.copy.content", Year.now()),
                    commonProperties.getBranding().getCopyrightIconUrl());
        }
        return builder;
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
    public void onEmbedMessage(MessageChannel sourceChannel, String code, Object... args) {
        EmbedBuilder builder = getBaseEmbed();
        builder.setDescription(getMessage(code, args));
        sendMessageSilent(sourceChannel::sendMessage, builder.build());
    }

    @Override
    public void onTempEmbedMessage(MessageChannel sourceChannel, int sec, String code, Object... args) {
        EmbedBuilder builder = getBaseEmbed();
        builder.setDescription(getMessage(code, args));
        sendTempMessageSilent(sourceChannel::sendMessage, builder.build(), sec);
    }

    @Override
    public void onTempMessage(MessageChannel sourceChannel, int sec, String code, Object... args) {
        onTempPlainMessage(sourceChannel, sec, getMessage(code, args));
    }

    @Override
    public void onTempPlainMessage(MessageChannel sourceChannel, int sec, String text) {
        sendTempMessageSilent(sourceChannel::sendMessage, text, sec);
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
                .setColor(contextService.getColor())
                .setDescription(getMessage(code, args));
    }

    @Override
    public String getMessage(String key, Object... args) {
        return getMessageByLocale(key, contextService.getLocale(), args);
    }

    @Override
    public String getMessageByLocale(String key, Locale locale, Object... args) {
        if (key == null) {
            return null;
        }
        if (locale == null) {
            locale = contextService.getLocale();
        }
        return context.getMessage(key, args, key, locale);
    }

    @Override
    public String getMessageByLocale(String key, String locale, Object... args) {
        return getMessageByLocale(key, StringUtils.isNotEmpty(locale)
                ? contextService.getLocale(locale) : contextService.getLocale(), args);
    }

    @Override
    public boolean hasMessage(String code) {
        return StringUtils.isNotEmpty(code) &&
                context.getMessage(code, null, null,
                        contextService.getLocale()) != null;
    }

    @Override
    public <T> void sendTempMessageSilent(Function<T, RestAction<Message>> action, T embed, int sec) {
        sendMessageSilentQueue(action, embed, message -> {
            JDA jda = message.getJDA();
            long messageId = message.getIdLong();
            long channelId = message.getChannel().getIdLong();
            ChannelType type = message.getChannelType();
            scheduler.schedule(() -> {
                MessageChannel channel = DiscordUtils.getChannel(jda, type, channelId);
                if (channel != null) {
                    channel.retrieveMessageById(messageId).queue(this::delete);
                }
            }, new DateTime().plusSeconds(sec).toDate());
        });
    }

    @Override
    public <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed) {
        sendMessageSilentQueue(action, embed, null);
    }

    @Override
    public <T> void sendMessageSilentQueue(Function<T, RestAction<Message>> action, T embed,
                                           Consumer<Message> messageConsumer) {
        try {
            action.apply(embed).queue(messageConsumer);
        } catch (PermissionException e) {
            // we don't care about it, this is why it is silent
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> T getEnumeration(Class<T> clazz, String title, String locale) {
        initCache(clazz);
        Locale targetLocale = contextService.getLocale(locale);
        if (targetLocale == null) {
            targetLocale = contextService.getLocale();
        }
        Map<String, Enum<?>> localizations = enumCache.get(clazz).get(targetLocale);
        if (localizations == null) {
            return null;
        }
        return (T) localizations.get(title.toLowerCase());
    }

    @Override
    public <T extends Enum<T>> T getEnumeration(Class<T> clazz, String title) {
        return getEnumeration(clazz, title, null);
    }

    @Override
    public String getEnumTitle(Enum<?> value) {
        return getEnumTitle(value, null);
    }

    @Override
    public String getEnumTitle(Enum<?> value, String locale) {
        if (value == null) {
            return null;
        }
        return getMessageByLocale(value.getClass().getName() + "." + value.name(), locale);
    }

    @Override
    public String getCountPlural(long count, String code) {
        String key = PluralUtils.getPluralKey(contextService.getLocale(), count);
        return getMessage(String.format("%s[%s]", code, key));
    }

    @Override
    public void delete(Message message) {
        if (message != null) {
            actionsHolderService.markAsDeleted(message);
            message.delete().queue();
        }
    }

    @Override
    public void delete(Message message, long delayMs) {
        if (message != null) {
            actionsHolderService.markAsDeleted(message);
            message.delete().queueAfter(delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void initCache(Class<?> clazz) {
        enumCache.computeIfAbsent(clazz, e -> {
            Map<Locale, Map<String, Enum<?>>> locales = new HashMap<>();
            Stream.of(clazz.getEnumConstants()).forEach(c -> {
                Enum<?> entry = (Enum<?>) c;
                for (var locale : LocaleUtils.SUPPORTED_LOCALES.entrySet()) {
                    Map<String, Enum<?>> localizations = locales.computeIfAbsent(locale.getValue(), v -> new HashMap<>());
                    localizations.put(getEnumTitle(entry, locale.getKey()).toLowerCase(), entry);
                }
            });
            return locales;
        });
    }
}
