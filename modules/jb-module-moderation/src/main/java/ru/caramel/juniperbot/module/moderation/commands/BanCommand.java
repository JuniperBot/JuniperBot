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
package ru.caramel.juniperbot.module.moderation.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.mod.ban.key",
        description = "discord.command.mod.ban.desc",
        group = "discord.command.group.moderation",
        source = ChannelType.TEXT,
        permissions = {
                Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.BAN_MEMBERS
        },
        priority = 25)
public class BanCommand extends ModeratorCommand {

    private final static Pattern BAN_PATTERN = Pattern.compile("([0-9]*)\\s*(.*)");

    @Override
    public boolean doCommand(MessageReceivedEvent event, BotContext context, String query) {
        Member mentioned = getMentioned(event);
        if (mentioned != null) {
            if (moderationService.isModerator(mentioned) || Objects.equals(mentioned, event.getMember())) {
                return fail(event); // do not allow ban members or yourself
            }

            if (!event.getGuild().getSelfMember().canInteract(mentioned)) {
                messageService.onError(event.getChannel(), "discord.command.mod.ban.position");
                return false;
            }

            query = removeMention(query);
            if (StringUtils.isNotEmpty(query)) {
                Matcher matcher = BAN_PATTERN.matcher(query);
                if (matcher.find()) {
                    int delDays = 0;
                    if (StringUtils.isNotEmpty(matcher.group(1))) {
                        delDays = Integer.parseInt(matcher.group(1));
                    }
                    moderationService.ban(event.getMember(), mentioned, delDays, matcher.group(2));
                    return ok(event);
                }
            } else {
                moderationService.ban(event.getMember(), mentioned);
                return ok(event);
            }
        }

        messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.ban.help",
                context.getConfig().getPrefix());
        return false;
    }
}
