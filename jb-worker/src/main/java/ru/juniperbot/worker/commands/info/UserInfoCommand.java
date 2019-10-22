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
package ru.juniperbot.worker.commands.info;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.LocalUser;
import ru.juniperbot.common.persistence.entity.MemberBio;
import ru.juniperbot.common.persistence.repository.MemberBioRepository;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.utils.CommonUtils;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.DiscordCommand;
import ru.juniperbot.common.worker.command.model.MemberReference;
import ru.juniperbot.common.worker.command.model.MentionableCommand;
import ru.juniperbot.common.worker.utils.DiscordUtils;
import ru.juniperbot.module.ranking.commands.RankCommand;
import ru.juniperbot.module.ranking.service.RankingService;

import java.util.Iterator;
import java.util.Objects;

@DiscordCommand(key = "discord.command.user.key",
        description = "discord.command.user.desc",
        group = "discord.command.group.info",
        priority = 5)
public class UserInfoCommand extends MentionableCommand {

    @Autowired
    private RankingConfigService rankingConfigService;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private RankCommand rankCommand;

    @Autowired
    private MemberBioRepository bioRepository;

    public UserInfoCommand() {
        super(true);
    }

    @Override
    public boolean doCommand(MemberReference reference, GuildMessageReceivedEvent event, BotContext context, String query) {
        DateTimeFormatter formatter = DateTimeFormat.mediumDateTime()
                .withLocale(contextService.getLocale())
                .withZone(context.getTimeZone());

        if (reference.getLocalMember() == null) {
            return fail(event);
        }

        LocalMember localMember = reference.getLocalMember();
        LocalUser localUser = reference.getLocalUser();

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle(messageService.getMessage("discord.command.user.title", localMember.getEffectiveName()));
        builder.setThumbnail(localUser.getAvatarUrl());
        builder.setFooter(messageService.getMessage("discord.command.info.identifier", reference.getId()), null);

        StringBuilder commonBuilder = new StringBuilder();
        getName(commonBuilder, localUser, localMember);

        if (reference.getMember() != null) {
            Member member = reference.getMember();
            getOnlineStatus(commonBuilder, member);
            if (CollectionUtils.isNotEmpty(member.getActivities())) {
                getActivities(commonBuilder, member);
            }
            if (member.getOnlineStatus() == OnlineStatus.OFFLINE || member.getOnlineStatus() == OnlineStatus.INVISIBLE) {
                if (localUser.getLastOnline() != null) {
                    getLastOnlineAt(commonBuilder, localUser, formatter);
                }
            }
            getJoinedAt(commonBuilder, member, formatter);
            getCreatedAt(commonBuilder, member.getUser(), formatter);
        }

        builder.addField(messageService.getMessage("discord.command.user.common"), commonBuilder.toString(), false);

        Guild guild = event.getGuild();

        if (rankingConfigService.isEnabled(guild.getIdLong())) {
            RankingInfo info = rankingService.getRankingInfo(localMember);
            rankCommand.addFields(builder, info, guild);
        }

        MemberBio memberBio = bioRepository.findByGuildIdAndUserId(guild.getIdLong(), reference.getId());
        String bio = memberBio != null ? memberBio.getBio() : null;
        if (StringUtils.isEmpty(bio)
                && Objects.equals(event.getAuthor(), reference.getUser())
                && !commandsService.isRestricted(BioCommand.KEY, event.getChannel(), event.getMember())) {
            String bioCommand = messageService.getMessageByLocale("discord.command.bio.key",
                    context.getCommandLocale());
            bio = messageService.getMessage("discord.command.user.bio.none", context.getConfig().getPrefix(),
                    bioCommand);
        }
        if (StringUtils.isNotEmpty(bio)) {
            builder.setDescription(CommonUtils.trimTo(bio, MessageEmbed.TEXT_MAX_LENGTH));
        }
        messageService.sendMessageSilent(event.getChannel()::sendMessage, builder.build());
        return true;
    }

    @Override
    protected void showHelp(GuildMessageReceivedEvent event, BotContext context) {
        fail(event);
    }

    private StringBuilder getName(StringBuilder commonBuilder, LocalUser user, LocalMember member) {
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

    private StringBuilder getLastOnlineAt(StringBuilder commonBuilder, LocalUser user, DateTimeFormatter formatter) {
        String lastOnlineString = new PrettyTime(contextService.getLocale()).format(user.getLastOnline());
        String dateTime = formatter.print(new DateTime(user.getLastOnline()));
        return appendEntry(commonBuilder, "discord.command.user.lastOnlineAt",
                String.format("%s (%s)", dateTime, lastOnlineString));
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
