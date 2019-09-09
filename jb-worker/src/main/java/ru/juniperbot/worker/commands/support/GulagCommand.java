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
package ru.juniperbot.worker.commands.support;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.service.GulagService;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.SupportCommand;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DiscordCommand(key = "discord.command.gulag.key",
        description = "discord.command.gulag.desc",
        group = "discord.command.group.utility",
        hidden = true,
        priority = 0)
public class GulagCommand extends SupportCommand {

    private static final Pattern PATTERN = Pattern.compile("^([0-9]+)\\s+(.+)$");

    @Autowired
    private GulagService gulagService;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent event, BotContext context, String content) {
        TextChannel channel = event.getChannel();
        Long showflake = null;
        String reason = null;
        User targetUser = null;
        Member targetMember = getMentioned(event);
        if (targetMember != null) {
            showflake = targetMember.getIdLong();
            targetUser = targetMember.getUser();
            reason = removeMention(content);
        } else {
            Matcher matcher = PATTERN.matcher(content);
            if (matcher.find()) {
                showflake = Long.valueOf(matcher.group(1));
                targetUser = discordService.getUserById(showflake);
                targetMember = event.getGuild().getMemberById(showflake);
                reason = matcher.group(2);
            }
        }
        if (showflake == null || StringUtils.isBlank(reason)) {
            return showHelp(channel, context);
        }

        if (targetMember != null && supportService.isModerator(targetMember)) {
            return fail(event);
        }

        if (targetUser != null && discordService.isSuperUser(targetUser)) {
            return fail(event);
        }

        String targetName = targetUser != null
                ? String.format("%s#%s (%s)", targetUser.getName(), targetUser.getDiscriminator(), showflake)
                : String.valueOf(showflake);

        boolean success = gulagService.send(event.getMember(), showflake, reason);
        if (!success) {
            messageService.onEmbedMessage(channel, "discord.command.gulag.exists", targetName);
            return false;
        }

        messageService.onEmbedMessage(channel, "discord.command.gulag.success", targetName);

        if (targetMember != null) {
            targetMember.ban(1, reason).queueAfter(30, TimeUnit.SECONDS);
        } if (targetUser != null) {
            event.getGuild().ban(targetUser, 1, reason).queue();
        }

        final User finalUser = targetUser;
        if (finalUser != null) {
            discordService.getShardManager().getGuildCache()
                    .stream()
                    .filter(e -> e.getOwner() != null && finalUser.equals(e.getOwner().getUser()))
                    .forEach(e -> e.leave().queue());
        }
        return true;
    }

    private boolean showHelp(TextChannel channel, BotContext context) {
        String command = messageService.getMessageByLocale("discord.command.gulag.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(channel, "discord.command.gulag.help",
                context.getConfig().getPrefix(), command);
        return false;
    }
}
