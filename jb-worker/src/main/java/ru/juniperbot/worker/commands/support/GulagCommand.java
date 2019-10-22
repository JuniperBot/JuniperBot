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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.service.GulagService;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.command.model.MentionableCommand;
import ru.juniperbot.common.worker.shared.service.SupportService;

import java.util.concurrent.TimeUnit;

@DiscordCommand(key = "discord.command.gulag.key",
        description = "discord.command.gulag.desc",
        group = "discord.command.group.utility",
        hidden = true,
        priority = 0)
public class GulagCommand extends MentionableCommand {

    @Autowired
    protected SupportService supportService;

    @Autowired
    private GulagService gulagService;

    public GulagCommand() {
        super(false, false);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String content) {
        TextChannel channel = event.getChannel();

        if (StringUtils.isBlank(content)) {
            showHelp(event, context);
            return false;
        }

        Member targetMember = reference.getMember();
        if (targetMember != null && supportService.isModerator(targetMember)) {
            return fail(event);
        }

        User targetUser = reference.getUser();
        if (targetUser != null && discordService.isSuperUser(targetUser)) {
            return fail(event);
        }

        String id = reference.getId();

        String targetName = targetUser != null
                ? String.format("%s#%s (%s)", targetUser.getName(), targetUser.getDiscriminator(), id)
                : id;

        boolean success = gulagService.send(event.getMember(), Long.valueOf(reference.getId()), content);
        if (!success) {
            messageService.onEmbedMessage(channel, "discord.command.gulag.exists", targetName);
            return false;
        }

        messageService.onEmbedMessage(channel, "discord.command.gulag.success", targetName);

        if (targetMember != null) {
            targetMember.ban(1, content).queueAfter(30, TimeUnit.SECONDS);
        } else if (targetUser != null) {
            event.getGuild().ban(targetUser, 1, content).queue();
        }

        if (targetUser != null) {
            discordService.getShardManager().getGuildCache()
                    .stream()
                    .filter(e -> e.getOwner() != null && targetUser.equals(e.getOwner().getUser()))
                    .forEach(e -> e.leave().queue());
        }
        return true;
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return member != null && guild != null
                && guild.equals(supportService.getSupportGuild())
                && supportService.isModerator(member);
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        String command = messageService.getMessageByLocale("discord.command.gulag.key",
                context.getCommandLocale());
        messageService.onEmbedMessage(event.getChannel(), "discord.command.gulag.help",
                context.getConfig().getPrefix(), command);
    }
}
