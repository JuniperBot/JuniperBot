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
package ru.caramel.juniperbot.module.welcome.listeners;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.core.listeners.DiscordEventListener;
import ru.caramel.juniperbot.core.model.DiscordEvent;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MemberService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.core.service.MessageTemplateService;
import ru.caramel.juniperbot.core.utils.DiscordUtils;
import ru.caramel.juniperbot.module.ranking.model.Reward;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.module.ranking.service.RankingService;
import ru.caramel.juniperbot.module.welcome.persistence.entity.WelcomeMessage;
import ru.caramel.juniperbot.module.welcome.service.WelcomeService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@DiscordEvent(priority = 10)
public class WelcomeUserListener extends DiscordEventListener {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private WelcomeService welcomeService;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MessageTemplateService templateService;

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getUser().isBot()) {
            return;
        }
        Guild guild = event.getGuild();
        contextService.withContextAsync(guild, () -> {
            WelcomeMessage message = welcomeService.getByGuildId(guild.getIdLong());
            if (message == null) {
                return;
            }

            Set<Long> roleIdsToAdd = new HashSet<>();
            if (CollectionUtils.isNotEmpty(message.getJoinRoles())) {
                message.getJoinRoles().stream()
                        .filter(Objects::nonNull)
                        .forEach(roleIdsToAdd::add);
            }

            RankingConfig rankingInfo = rankingService.get(guild);
            if (rankingInfo != null && CollectionUtils.isNotEmpty(rankingInfo.getRewards())) {
                rankingInfo.getRewards().stream()
                        .filter(e -> e != null && e.getLevel() != null && e.getLevel().equals(0))
                        .map(Reward::getRoleId)
                        .filter(StringUtils::isNumeric)
                        .map(Long::valueOf)
                        .forEach(roleIdsToAdd::add);
            }

            LocalMember localMember = memberService.get(event.getMember());
            if (message.isRestoreState() && localMember != null) {
                if (CollectionUtils.isNotEmpty(localMember.getLastKnownRoles())) {
                    roleIdsToAdd.addAll(localMember.getLastKnownRoles());
                }

                if (StringUtils.isNotEmpty(localMember.getEffectiveName())
                        && guild.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)
                        && guild.getSelfMember().canInteract(event.getMember())) {
                    guild.getController().setNickname(event.getMember(), localMember.getEffectiveName()).queue();
                }
            }

            if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)
                    && guild.getSelfMember().canInteract(event.getMember())) {
                Set<Role> roles = roleIdsToAdd.stream()
                        .map(guild::getRoleById)
                        .filter(e -> e != null && guild.getSelfMember().canInteract(e) && !e.isManaged())
                        .collect(Collectors.toSet());
                if (!roles.isEmpty()) {
                    guild.getController().addRolesToMember(event.getMember(), roles).queue();
                }
            }

            if (message.isJoinEnabled() && message.getJoinTemplate() != null) {
                TextChannel channel = DiscordUtils.getDefaultWriteableChannel(event.getGuild());
                templateService
                        .createMessage(message.getJoinTemplate())
                        .withFallbackContent("welcome.join.message")
                        .withGuild(guild)
                        .withMember(event.getMember())
                        .withFallbackChannel(channel)
                        .compileAndSend();
            }

            if (message.isJoinDmEnabled() && message.getJoinDmTemplate() != null) {
                Message compiledMessage = templateService
                        .createMessage(message.getJoinDmTemplate())
                        .withFallbackContent("welcome.join.dm.message")
                        .withGuild(guild)
                        .withMember(event.getMember())
                        .compile();

                if (compiledMessage != null) {
                    User user = event.getUser();
                    try {
                        contextService.queue(guild, user.openPrivateChannel(),
                                c -> messageService.sendMessageSilent(c::sendMessage, compiledMessage));
                    } catch (Exception e) {
                        log.debug("Could not open private channel for user {}", user, e);
                    }
                }
            }
        });
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getUser().isBot()) {
            return;
        }
        Guild guild = event.getGuild();
        contextService.withContextAsync(guild, () -> {
            WelcomeMessage message = welcomeService.getByGuildId(event.getGuild().getIdLong());
            if (message == null || !message.isLeaveEnabled() || message.getLeaveTemplate() == null) {
                return;
            }
            TextChannel channel = DiscordUtils.getDefaultWriteableChannel(event.getGuild());
            templateService
                    .createMessage(message.getLeaveTemplate())
                    .withFallbackContent("welcome.leave.message")
                    .withGuild(guild)
                    .withFallbackChannel(channel)
                    .withMember(event.getMember())
                    .compileAndSend();
        });
    }
}