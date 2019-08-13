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
package ru.caramel.juniperbot.module.ranking.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.DiscordCommand;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;

@DiscordCommand(
        key = "discord.command.rank.key",
        description = "discord.command.rank.desc",
        group = "discord.command.group.ranking",
        priority = 202)
public class RankCommand extends RankingCommand {

    @Override
    protected boolean doInternal(GuildMessageReceivedEvent message, BotContext context, String content) {
        Member member = message.getMember();
        if (CollectionUtils.isNotEmpty(message.getMessage().getMentionedUsers())) {
            member = message.getGuild().getMember(message.getMessage().getMentionedUsers().get(0));
        }
        RankingInfo info = rankingService.getRankingInfo(member);
        if (info == null) {
            messageService.sendMessageSilent(message.getChannel()::sendMessage, messageService.getMessage(
                    "discord.command.rank.unavailable"));
            return false;
        }

        if (message.getGuild().getSelfMember().hasPermission(message.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            EmbedBuilder builder = messageService.getBaseEmbed(true);
            addFields(builder, info, member.getGuild());

            long desiredPage = (info.getRank() / 50) + 1;
            String url = String.format("https://juniper.bot/ranking/%s?page=%s#%s", member.getGuild().getId(),
                    desiredPage, member.getUser().getId());
            builder.setAuthor(member.getEffectiveName(), url, member.getUser().getAvatarUrl());
            messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
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
            messageService.sendMessageSilent(message.getChannel()::sendMessage, response);
        }
        return true;
    }

    public void addFields(EmbedBuilder builder, RankingInfo info, Guild guild) {
        RankingConfig config = rankingService.get(guild);
        long totalMembers = rankingService.countRankings(guild.getIdLong());
        builder.addField(messageService.getMessage("discord.command.rank.info.rank.title"),
                String.format("# %d/%d", info.getRank(), totalMembers), true);
        builder.addField(messageService.getMessage("discord.command.rank.info.lvl.title"),
                String.valueOf(info.getLevel()), true);
        builder.addField(messageService.getMessage("discord.command.rank.info.exp.title"),
                messageService.getMessage("discord.command.rank.info.exp.format",
                        info.getRemainingExp(), info.getLevelExp(), info.getTotalExp()), true);
        if (config != null && config.isCookieEnabled()) {
            builder.addField(messageService.getMessage("discord.command.rank.info.cookies.title"),
                    String.format("%d \uD83C\uDF6A", info.getCookies()), true);
        }
    }
}
