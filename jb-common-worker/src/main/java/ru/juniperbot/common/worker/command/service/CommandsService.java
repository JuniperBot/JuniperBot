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
package ru.juniperbot.common.worker.command.service;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.entity.GuildConfig;

/**
 * Common commands interface
 *
 * @see InternalCommandsService
 * @see CustomCommandsServiceImpl
 */
public interface CommandsService {

    /**
     * Checks if specified command valid
     *
     * @param event Message event
     * @param key   Command key
     * @return Is command key valid
     */
    boolean isValidKey(GuildMessageReceivedEvent event, String key);

    /**
     * Sends command
     *
     * @param event       Message event
     * @param content     Command content
     * @param key         Command key
     * @param guildConfig GuildConfig of guild invoked this command
     * @return Is command was sent
     */
    boolean sendCommand(GuildMessageReceivedEvent event, String content, String key, GuildConfig guildConfig);

    /**
     * Adds an emoji to original message
     *
     * @param message     Message
     * @param emoji       Emoji code
     * @param messageCode Fallback message code
     * @param args        Arguments for fallback message code
     */
    void resultEmotion(GuildMessageReceivedEvent message, String emoji, String messageCode, Object... args);

    /**
     * Checks is command has restrictions for this TextChannel
     *
     * @param commandConfig Command configuration
     * @param channel       Channel to check
     * @return Is restricted
     */
    boolean isRestricted(CommandConfig commandConfig, TextChannel channel);

    /**
     * Checks is command has restrictions for this member
     *
     * @param commandConfig Command configuration
     * @param member        Member to check
     * @return Is restricted
     */
    boolean isRestricted(CommandConfig commandConfig, Member member);

    /**
     * Checks all restrictions for command invocation
     *
     * @param event         Message event
     * @param commandConfig Command configuration
     * @return Is restricted
     */
    boolean isRestricted(GuildMessageReceivedEvent event, CommandConfig commandConfig);

}
