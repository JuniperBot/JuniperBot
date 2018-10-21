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
package ru.caramel.juniperbot.core.commands.moderation;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.mod.mute.key",
        description = "discord.command.mod.mute.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS, Permission.VOICE_MUTE_OTHERS},
        priority = 30)
public class MuteCommand extends ModeratorCommandAsync {

    private static final String COMMAND_PATTERN = "^(\\d+\\s*)?(%s\\s*)?(.*)$";

    @Override
    protected void doCommandAsync(MessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (moderationService.isModerator(mentioned) || Objects.equals(mentioned, event.getMember())) {
            return; // do not allow to mute moderators
        }

        String globalKeyWord = messageService.getMessageByLocale("discord.command.mod.mute.key.everywhere",
                context.getCommandLocale());

        if (mentioned == null) {
            help(event, context, globalKeyWord);
            return;
        }

        Matcher m = Pattern
                .compile(String.format(COMMAND_PATTERN, Pattern.quote(globalKeyWord)))
                .matcher(removeMention(query));
        if (!m.find()) {
            help(event, context, globalKeyWord);
            return;
        }

        Integer duration = null;
        if (StringUtils.isNotBlank(m.group(1))) {
            try {
                duration = Integer.parseInt(m.group(1).trim());
            } catch (NumberFormatException e) {
                help(event, context, globalKeyWord);
                return;
            }
        }
        boolean global = m.group(2) != null && globalKeyWord.equals(m.group(2).trim());
        boolean muted = moderationService.mute(event.getTextChannel(), mentioned, global, duration, m.group(3));
        messageService.onEmbedMessage(event.getChannel(), muted
                ? "discord.command.mod.mute.done" : "discord.command.mod.mute.already", mentioned.getEffectiveName());
    }

    private void help(MessageReceivedEvent event, BotContext context, String globalKeyWord) {
        String muteCommand = messageService.getMessageByLocale("discord.command.mod.mute.key",
                context.getCommandLocale());
        messageService.onMessage(event.getChannel(),
                "discord.command.mod.mute.mention",
                context.getConfig().getPrefix(), muteCommand, globalKeyWord);
    }
}
