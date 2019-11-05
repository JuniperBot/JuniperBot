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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.MemberWarning;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.command.model.MentionableCommand;
import ru.juniperbot.common.worker.modules.moderation.service.ModerationService;

import java.util.List;

@DiscordCommand(key = "discord.command.mod.warns.key",
        description = "discord.command.mod.warns.desc",
        group = {"discord.command.group.moderation", "discord.command.group.utility"},
        priority = 15)
public class WarnsCommand extends MentionableCommand {

    @Autowired
    private ModerationService moderationService;

    public WarnsCommand() {
        super(true, true);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        LocalMember member = reference.getLocalMember();

        List<MemberWarning> warningList = moderationService.getWarnings(member);
        if (warningList.isEmpty()) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warns.empty");
            return false;
        }
        if (warningList.size() > 20) {
            warningList = warningList.subList(0, 20);
        }

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.mod.warns.message.title", member.getEffectiveName()));

        DateTimeFormatter formatter = DateTimeFormat
                .shortDateTime()
                .withZone(context.getTimeZone())
                .withLocale(contextService.getLocale());

        int i = 1;
        int length = builder.length();
        for (MemberWarning warning : warningList) {
            String title = String.format("%2s. %s %s (%s)", i++,
                    formatter.print(new DateTime(warning.getDate())),
                    CommonUtils.getUTCOffset(context.getTimeZone()),
                    warning.getModerator().getEffectiveName());
            StringBuilder detailsBuilder = new StringBuilder();
            if (StringUtils.isNotEmpty(warning.getReason())) {
                detailsBuilder.append(messageService.getMessage("discord.command.mod.warns.reason",
                        warning.getReason()));
            }
            if (warning.getEndDate() != null) {
                if (detailsBuilder.length() > 0) {
                    detailsBuilder.append("\n");
                }
                detailsBuilder.append(messageService.getMessage("discord.command.mod.warns.until",
                        formatter.print(new DateTime(warning.getEndDate())),
                        CommonUtils.getUTCOffset(context.getTimeZone())));
            }
            if ((length += title.length() + detailsBuilder.length()) > MessageEmbed.EMBED_MAX_LENGTH_BOT) {
                break;
            }
            builder.addField(title, detailsBuilder.toString(), false);
        }
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
        return true;
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return true; // to everyone
    }
}
