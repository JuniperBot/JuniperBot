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
package ru.juniperbot.worker.commands.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.mod.color.key",
        description = "discord.command.mod.color.desc",
        group = {"discord.command.group.moderation", "discord.command.group.utility"},
        permissions = {Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_ROLES},
        priority = 5)
public class ColorCommand extends MentionableModeratorCommand {

    private static final Pattern COLOR_PATTERN = Pattern.compile("([0-9a-fA-F]{6})$");

    protected ColorCommand() {
        super(true, true);
    }

    @Override
    protected boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        boolean moderator = moderationService.isModerator(event.getMember());
        Member member = moderator ? reference.getMember() : event.getMember();
        if (member == null) {
            return fail(event);
        }

        String removeKeyWord = messageService.getMessage("discord.command.mod.color.remove");
        if (StringUtils.endsWithIgnoreCase(query, removeKeyWord)) {
            if (moderationService.setColor(member, null)) {
                ok(event);
            } else {
                fail(event);
            }
            return false;
        }

        Matcher matcher = COLOR_PATTERN.matcher(query);
        if (!matcher.find()) {
            String colorCommand = messageService.getMessageByLocale("discord.command.mod.color.key",
                    context.getCommandLocale());
            String message = messageService.getMessage("discord.command.mod.color.help",
                    context.getConfig().getPrefix(), colorCommand);
            if (moderator) {
                message += "\n" + messageService.getMessage("discord.command.mod.color.help.mod",
                        context.getConfig().getPrefix(), colorCommand);
            }
            messageService.onEmbedMessage(event.getChannel(), message);
            return false;
        }

        Member self = event.getGuild().getSelfMember();
        Role conflicting = member.getRoles().stream()
                .filter(e -> e.getColor() != null && !self.canInteract(e))
                .findAny().orElse(null);
        if (conflicting != null) {
            messageService.onError(event.getChannel(), null, "discord.command.mod.color.conflict", conflicting.getName());
            return false;
        }

        if (moderationService.setColor(member, matcher.group(1))) {
            ok(event);
        }
        return true;
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return super.isAvailable(user, member, guild) || (guild != null &&
                moderationService.isPublicColor(guild.getIdLong()));
    }
}
