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
package ru.caramel.juniperbot.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.function.Function;

public interface MessageService {

    EmbedBuilder getBaseEmbed();

    <T> void sendMessageSilent(Function<T, RestAction<Message>> action, T embed);

    void onMessage(MessageChannel sourceChannel, String code, Object... args);

    void onTitledMessage(MessageChannel sourceChannel, String titleCode, String code, Object... args);

    void onError(MessageChannel sourceChannel, String code, Object... args);

    void onError(MessageChannel sourceChannel, String titleCode, String code, Object... args);

    String getMessage(String code, Object... args);

    boolean hasMessage(String code);

    <T extends Enum<T>> T getEnumeration(Class<T> clazz, String title);

    String getEnumTitle(Enum<?> clazz);
}
