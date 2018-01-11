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
package ru.caramel.juniperbot.core.modules.ranking.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.model.enums.CommandGroup;
import ru.caramel.juniperbot.core.model.enums.CommandSource;
import ru.caramel.juniperbot.core.model.exception.DiscordException;
import ru.caramel.juniperbot.core.modules.ranking.model.RankingInfo;

@DiscordCommand(
        key = "discord.command.rank.key",
        description = "discord.command.rank.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.RANKING,
        priority = 202)
public class RankCommand extends RankingCommand {

    @Override
    protected boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        Member member = message.getMember();
        if (CollectionUtils.isNotEmpty(message.getMessage().getMentionedUsers())) {
            member = message.getGuild().getMember(message.getMessage().getMentionedUsers().get(0));
        }
        RankingInfo info = rankingService.getRankingInfo(member);
        if (info == null) {
            messageService.sendMessageSilent(message.getTextChannel()::sendMessage, messageService.getMessage(
                    "discord.command.rank.unavailable"));
            return false;
        }

        if (PermissionUtil.checkPermission(message.getTextChannel(), message.getGuild().getSelfMember(),
                Permission.MESSAGE_EMBED_LINKS)) {
            EmbedBuilder builder = messageService.getBaseEmbed();
            builder.addField(messageService.getMessage("discord.command.rank.info.rank.title"),
                    String.format("%d/%d", info.getRank(), info.getTotalMembers()), true);
            builder.addField(messageService.getMessage("discord.command.rank.info.lvl.title"),
                    String.valueOf(info.getLevel()), true);
            builder.addField(messageService.getMessage("discord.command.rank.info.exp.title"),
                    String.format("%d/%d (всего %d)", info.getRemainingExp(), info.getLevelExp(), info.getTotalExp()),
                    true);
            builder.setAuthor(member.getEffectiveName(), null, member.getUser().getAvatarUrl());
            if (StringUtils.isNotEmpty(messageService.getCopyContent())) {
                builder.setFooter(messageService.getCopyContent(), messageService.getCopyImageUrl());
            }
            messageService.sendMessageSilent(message.getTextChannel()::sendMessage, builder.build());
        } else {
            String response;
            if (message.getMember().equals(member)) {
                response = messageService.getMessage("discord.command.rank.info.message.self",
                        member.getAsMention(),
                        info.getRank(),
                        info.getLevel(),
                        info.getRemainingExp(),
                        info.getLevelExp(),
                        info.getTotalExp());
            } else {
                response = messageService.getMessage("discord.command.rank.info.message.member",
                        message.getMember().getAsMention(),
                        member.getAsMention(),
                        info.getRank(),
                        info.getLevel(),
                        info.getRemainingExp(),
                        info.getLevelExp(),
                        info.getTotalExp());
            }
            messageService.sendMessageSilent(message.getTextChannel()::sendMessage, response);
        }
        return true;
    }
}
