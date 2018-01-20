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
package ru.caramel.juniperbot.module.info.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.model.BotContext;
import ru.caramel.juniperbot.core.model.DiscordCommand;
import ru.caramel.juniperbot.core.utils.CommonUtils;
import ru.caramel.juniperbot.module.info.persistence.entity.MemberBio;
import ru.caramel.juniperbot.module.info.persistence.repository.MemberBioRepository;
import ru.caramel.juniperbot.module.ranking.commands.RankCommand;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.service.RankingService;

import java.util.Objects;

@DiscordCommand(key = "discord.command.user.key",
        description = "discord.command.user.desc",
        group = "discord.command.group.info",
        priority = 5)
public class UserInfoCommand extends InfoCommand {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RankCommand rankCommand;

    @Autowired
    private MemberBioRepository bioRepository;

    @Override
    public boolean doCommand(MessageReceivedEvent message, BotContext context, String query) {
        DateTimeFormatter formatter = DateTimeFormat.fullDateTime().withLocale(contextService.getLocale());
        User author = message.getAuthor();
        User user = author;
        if (!message.getMessage().getMentionedUsers().isEmpty()) {
            user = message.getMessage().getMentionedUsers().get(0);
        }
        Member member = null;
        if (message.getGuild() != null) {
            member = message.getGuild().getMember(user);
        }

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.user.title",
                member != null ? member.getEffectiveName() : user.getName()));
        builder.setThumbnail(user.getEffectiveAvatarUrl());
        builder.setFooter(messageService.getMessage("discord.command.info.identifier", user.getId()), null);

        StringBuilder commonBuilder = new StringBuilder();
        getName(commonBuilder, user, member);

        if (member != null) {
            getOnlineStatus(commonBuilder, member);
            if (member.getGame() != null) {
                getGame(commonBuilder, member);
            }
            getJoinedAt(commonBuilder, member, formatter);
        }
        getCreatedAt(commonBuilder, user, formatter);

        builder.addField(messageService.getMessage("discord.command.user.common"), commonBuilder.toString(), false);

        if (member != null && !user.isBot()) {
            if (rankingService.isEnabled(member.getGuild().getIdLong())) {
                RankingInfo info = rankingService.getRankingInfo(member);
                if (info != null) {
                    rankCommand.addFields(builder, info, member.getGuild());
                }
            }
            MemberBio memberBio = bioRepository.findByGuildIdAndUserId(member.getGuild().getId(), user.getId());
            String bio = memberBio != null ? memberBio.getBio() : null;
            if (StringUtils.isEmpty(bio) && Objects.equals(author, user)) {
                bio = messageService.getMessage("discord.command.user.bio.none", context.getConfig().getPrefix());
            }
            builder.setDescription(CommonUtils.trimTo(bio, MessageEmbed.TEXT_MAX_LENGTH));
        }
        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    private StringBuilder getName(StringBuilder commonBuilder, User user, Member member) {
        String userName = CommonUtils.formatUser(user);
        if (member != null && !Objects.equals(user.getName(), member.getEffectiveName())) {
            userName += String.format(" (%s)", member.getEffectiveName());
        }
        return appendEntry(commonBuilder, "discord.command.user.username", userName);
    }

    private StringBuilder getCreatedAt(StringBuilder commonBuilder, User user, DateTimeFormatter formatter) {
        return appendEntry(commonBuilder, "discord.command.user.createdAt", user.getCreationTime().toEpochSecond(), formatter);
    }

    private StringBuilder getJoinedAt(StringBuilder commonBuilder, Member member, DateTimeFormatter formatter) {
        return appendEntry(commonBuilder, "discord.command.user.joinedAt", member.getJoinDate().toEpochSecond(), formatter);
    }

    private StringBuilder getOnlineStatus(StringBuilder commonBuilder, Member member) {
        return appendEntry(commonBuilder, "discord.command.user.status",
                messageService.getEnumTitle(member.getOnlineStatus()));
    }

    private StringBuilder getGame(StringBuilder commonBuilder, Member member) {
        Game game = member.getGame();
        return appendEntry(commonBuilder, game.getType(), game.getName());
    }

    private StringBuilder appendEntry(StringBuilder commonBuilder, String name, String value) {
        return commonBuilder
                .append("**")
                .append(messageService.getMessage(name))
                .append(":** ")
                .append(value)
                .append("\n");
    }

    private StringBuilder appendEntry(StringBuilder commonBuilder, Enum<?> enumName, String value) {
        return commonBuilder
                .append("**")
                .append(enumName)
                .append(":** ")
                .append(value)
                .append("\n");
    }

    private StringBuilder appendEntry(StringBuilder commonBuilder, String nameKey, long epochSecond,
                                      DateTimeFormatter formatter) {
        DateTime dateTime = new DateTime(epochSecond * 1000).withZone(DateTimeZone.UTC);
        return appendEntry(commonBuilder, nameKey, formatter.print(dateTime));
    }
}
