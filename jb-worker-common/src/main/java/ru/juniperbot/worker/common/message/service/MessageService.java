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
package ru.juniperbot.worker.common.message.service;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MessageService {

    EmbedBuilder getBaseEmbed();

    EmbedBuilder getBaseEmbed(boolean copyright);

    <T> void sendTempMessageSilent(Function<T, RestAction<Message>> action, T embed, int sec);

    <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed);

    <T> void sendMessageSilentQueue(Function<T, RestAction<Message>> action, T embed,
                                    Consumer<Message> messageConsumer);

    void onMessage(MessageChannel sourceChannel, String code, Object... args);

    void onEmbedMessage(MessageChannel sourceChannel, String code, Object... args);

    void onTempEmbedMessage(MessageChannel sourceChannel, int sec, String code, Object... args);

    void onTempMessage(MessageChannel sourceChannel, int sec, String code, Object... args);

    void onTempPlainMessage(MessageChannel sourceChannel, int sec, String message);

    void onTitledMessage(MessageChannel sourceChannel, String titleCode, String code, Object... args);

    void onError(MessageChannel sourceChannel, String code, Object... args);

    void onError(MessageChannel sourceChannel, String titleCode, String code, Object... args);

    String getMessage(String code, Object... args);

    String getMessageByLocale(String code, Locale locale, Object... args);

    String getMessageByLocale(String key, String locale, Object... args);

    boolean hasMessage(String code);

    <T extends Enum<T>> T getEnumeration(Class<T> clazz, String title);

    String getEnumTitle(Enum<?> clazz);

    String getCountPlural(long count, String code);

    void delete(Message message);
}
