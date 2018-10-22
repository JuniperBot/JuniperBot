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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.core.persistence.entity.MemberWarning;

import java.util.List;

@DiscordCommand(key = "discord.command.mod.warns.key",
        description = "discord.command.mod.warns.desc",
        group = {"discord.command.group.moderation", "discord.command.group.utility"},
        source = ChannelType.TEXT,
        priority = 15)
public class WarnsCommand extends ModeratorCommandAsync {

    @Override
    public void doCommandAsync(MessageReceivedEvent event, BotContext context, String query) {
        Member member = getMentioned(event);
        if (member == null) {
            member = event.getMember();
        }

        List<MemberWarning> warningList = moderationService.getWarnings(member);
        if (warningList.isEmpty()) {
            messageService.onEmbedMessage(event.getChannel(), "discord.command.mod.warns.empty");
            return;
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
        for (MemberWarning warning : warningList) {
            StringBuilder entryBuilder = new StringBuilder();
            if (i > 1) {
                entryBuilder.append("\n");
            }
            entryBuilder
                    .append(String.format("`%2s. ", i++))
                    .append(formatter.print(new DateTime(warning.getDate())))
                    .append(" ")
                    .append(CommonUtils.getUTCOffset(context.getTimeZone()))
                    .append("` ")
                    .append(warning.getModerator().getEffectiveName());
            if (StringUtils.isNotEmpty(warning.getReason())) {
                entryBuilder.append(": ").append(warning.getReason());
            }
            builder.appendDescription(entryBuilder);
        }
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
    }

    @Override
    public boolean isAvailable(User user, Member member, Guild guild) {
        return true; // to everyone
    }
}
