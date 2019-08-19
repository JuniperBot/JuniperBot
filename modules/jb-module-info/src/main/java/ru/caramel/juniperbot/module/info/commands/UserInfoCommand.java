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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.worker.common.command.model.BotContext;
import ru.juniperbot.worker.common.command.model.DiscordCommand;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.worker.common.utils.DiscordUtils;
import ru.caramel.juniperbot.module.info.persistence.entity.MemberBio;
import ru.caramel.juniperbot.module.info.persistence.repository.MemberBioRepository;
import ru.caramel.juniperbot.module.ranking.commands.RankCommand;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.service.RankingService;

import java.util.Iterator;
import java.util.Objects;

@DiscordCommand(key = "discord.command.user.key",
        description = "discord.command.user.desc",
        group = "discord.command.group.info",
        priority = 5)
public class UserInfoCommand extends AbstractInfoCommand {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RankCommand rankCommand;

    @Autowired
    private MemberBioRepository bioRepository;

    @Override
    public boolean doCommand(GuildMessageReceivedEvent message, BotContext context, String query) {
        DateTimeFormatter formatter = DateTimeFormat.mediumDateTime()
                .withLocale(contextService.getLocale())
                .withZone(context.getTimeZone());
        User author = message.getAuthor();
        User user = author;
        if (!message.getMessage().getMentionedUsers().isEmpty()) {
            user = message.getMessage().getMentionedUsers().get(0);
        }
        if (!message.getGuild().isMember(user)) {
            return fail(message);
        }
        Member member = message.getGuild().getMember(user);

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.user.title", member.getEffectiveName()));
        builder.setThumbnail(user.getEffectiveAvatarUrl());
        builder.setFooter(messageService.getMessage("discord.command.info.identifier", user.getId()), null);

        StringBuilder commonBuilder = new StringBuilder();
        getName(commonBuilder, user, member);

        getOnlineStatus(commonBuilder, member);
        if (CollectionUtils.isNotEmpty(member.getActivities())) {
            getActivities(commonBuilder, member);
        }
        getJoinedAt(commonBuilder, member, formatter);
        getCreatedAt(commonBuilder, user, formatter);

        builder.addField(messageService.getMessage("discord.command.user.common"), commonBuilder.toString(), false);

        if (!user.isBot()) {
            if (rankingService.isEnabled(member.getGuild().getIdLong())) {
                RankingInfo info = rankingService.getRankingInfo(member);
                if (info != null) {
                    rankCommand.addFields(builder, info, member.getGuild());
                }
            }
            MemberBio memberBio = bioRepository.findByGuildIdAndUserId(member.getGuild().getIdLong(), user.getId());
            String bio = memberBio != null ? memberBio.getBio() : null;
            if (StringUtils.isEmpty(bio)
                    && Objects.equals(author, user)
                    && !commandsService.isRestricted(BioCommand.KEY, message.getChannel(), message.getMember())) {
                String bioCommand = messageService.getMessageByLocale("discord.command.bio.key",
                        context.getCommandLocale());
                bio = messageService.getMessage("discord.command.user.bio.none", context.getConfig().getPrefix(),
                        bioCommand);
            }
            if (StringUtils.isNotEmpty(bio)) {
                builder.setDescription(CommonUtils.trimTo(bio, MessageEmbed.TEXT_MAX_LENGTH));
            }
        }
        messageService.sendMessageSilent(message.getChannel()::sendMessage, builder.build());
        return true;
    }

    private StringBuilder getName(StringBuilder commonBuilder, User user, Member member) {
        String userName = DiscordUtils.formatUser(user);
        if (!Objects.equals(user.getName(), member.getEffectiveName())) {
            userName += String.format(" (%s)", member.getEffectiveName());
        }
        return appendEntry(commonBuilder, "discord.command.user.username", userName);
    }

    private StringBuilder getCreatedAt(StringBuilder commonBuilder, User user, DateTimeFormatter formatter) {
        return appendEntry(commonBuilder, "discord.command.user.createdAt", user.getTimeCreated().toEpochSecond(), formatter);
    }

    private StringBuilder getJoinedAt(StringBuilder commonBuilder, Member member, DateTimeFormatter formatter) {
        return appendEntry(commonBuilder, "discord.command.user.joinedAt", member.getTimeJoined().toEpochSecond(), formatter);
    }

    private StringBuilder getOnlineStatus(StringBuilder commonBuilder, Member member) {
        return appendEntry(commonBuilder, "discord.command.user.status",
                messageService.getEnumTitle(member.getOnlineStatus()));
    }

    private StringBuilder getActivities(StringBuilder commonBuilder, Member member) {
        Iterator<Activity> iterable = member.getActivities().iterator();
        while (iterable.hasNext()) {
            Activity activity = iterable.next();
            appendEntry(commonBuilder, activity.getType(), activity.getName());
            if (iterable.hasNext()) {
                commonBuilder.append("\n");
            }
        }
        return commonBuilder;
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
                .append(messageService.getEnumTitle(enumName))
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
