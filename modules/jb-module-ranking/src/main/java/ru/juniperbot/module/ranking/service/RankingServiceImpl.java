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
package ru.juniperbot.module.ranking.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.model.RankingReward;
import ru.juniperbot.common.persistence.entity.*;
import ru.juniperbot.common.persistence.repository.CookieRepository;
import ru.juniperbot.common.persistence.repository.RankingRepository;
import ru.juniperbot.common.service.MemberService;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.service.TransactionHandler;
import ru.juniperbot.common.utils.RankingUtils;
import ru.juniperbot.common.worker.feature.service.FeatureSetService;
import ru.juniperbot.common.worker.message.service.MessageTemplateService;
import ru.juniperbot.common.worker.shared.service.DiscordEntityAccessor;
import ru.juniperbot.common.worker.shared.service.DiscordService;
import ru.juniperbot.module.ranking.model.GainExpResult;
import ru.juniperbot.module.ranking.utils.VoiceActivityTracker;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RankingServiceImpl implements RankingService {

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private CookieRepository cookieRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageTemplateService templateService;

    @Autowired
    private DiscordEntityAccessor entityAccessor;

    @Autowired
    private RankingConfigService configService;

    @Autowired
    private FeatureSetService featureSetService;

    @Autowired
    private TransactionHandler transactionHandler;

    private static Object DUMMY = new Object();

    private Cache<String, Object> coolDowns = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    @Transactional
    @Override
    public void onMessage(GuildMessageReceivedEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        RankingConfig config = configService.get(guild);
        if (config == null
                || member == null
                || !config.isEnabled()
                || !memberService.isApplicable(member)
                || configService.isBanned(config, member)) {
            return;
        }

        String memberKey = String.format("%s_%s", guild.getId(), event.getAuthor().getId());
        long gainExp = coolDowns.getIfPresent(memberKey) == null && !isIgnoredChannel(config, event.getChannel())
                ? Math.round(RandomUtils.nextLong(15, 25) * config.getTextExpMultiplier()) : 0;

        boolean hasCookie = config.isCookieEnabled()
                && CollectionUtils.isNotEmpty(event.getMessage().getMentionedUsers())
                && StringUtils.isNotEmpty(event.getMessage().getContentRaw())
                && event.getMessage().getContentRaw().contains(RankingService.COOKIE_EMOTE);

        List<Member> cookieRecipients = hasCookie ?
                event.getMessage().getMentionedMembers().stream().filter(e -> !e.getUser().isBot()
                        && !Objects.equals(e.getUser(), event.getAuthor())
                        && memberService.isApplicable(e))
                        .collect(Collectors.toList())
                : null;

        if (gainExp == 0 && CollectionUtils.isEmpty(cookieRecipients)) {
            return;
        }

        updateRanking(config, member, gainExp, event.getChannel(), ranking -> {
            if (CollectionUtils.isNotEmpty(cookieRecipients)) {
                Date checkDate = getCookieCoolDown();
                cookieRecipients.forEach(recipientMember -> {
                    LocalMember recipient = entityAccessor.getOrCreate(recipientMember);
                    giveCookie(ranking.getMember(), recipient, checkDate);
                });
            }
        });
        if (gainExp > 0) {
            coolDowns.put(memberKey, DUMMY);
        }
    }

    @Transactional
    @Override
    public void giveCookie(Member senderMember, Member recipientMember) {
        if (!memberService.isApplicable(senderMember) || !memberService.isApplicable(recipientMember)) {
            return;
        }
        RankingConfig config = configService.get(senderMember.getGuild());
        if (config != null && config.isCookieEnabled()) {
            transactionHandler.runWithLockRetry(() -> {
                LocalMember recipient = entityAccessor.getOrCreate(recipientMember);
                LocalMember sender = entityAccessor.getOrCreate(senderMember);
                giveCookie(sender, recipient, getCookieCoolDown());
            });
        }
    }

    @Override
    @Transactional
    public void addVoiceActivity(Member member, VoiceActivityTracker.MemberState state) {
        if (!memberService.isApplicable(member)) {
            return;
        }
        RankingConfig config = configService.get(member.getGuild());
        if (config == null || !config.isEnabled()) {
            return;
        }
        long gainedExp = config.isVoiceEnabled()
                && featureSetService.isAvailable(member.getGuild())
                && !configService.isBanned(config, member)
                ? Math.round(15 * state.getPoints().get() * config.getVoiceExpMultiplier()) : 0;
        updateRanking(config, member, gainedExp, null, ranking -> {
            ranking.setVoiceActivity(ranking.getVoiceActivity() + state.getActivityTime().get());
        });
    }

    private void updateRanking(RankingConfig config,
                               Member member,
                               long gainedExp,
                               TextChannel notifyChannel,
                               Consumer<Ranking> preProcess) {
        GainExpResult result = transactionHandler.runWithLockRetry(() -> {
            Ranking ranking = getRanking(member);
            preProcess.accept(ranking);
            int oldLevel = RankingUtils.getLevelFromExp(ranking.getExp());
            ranking.setExp(ranking.getExp() + gainedExp);
            int newLevel = RankingUtils.getLevelFromExp(ranking.getExp());
            rankingRepository.save(ranking);
            return new GainExpResult(ranking, oldLevel, newLevel);
        });
        if (result.getOldLevel() < result.getNewLevel()) {
            if (config.isAnnouncementEnabled()) {
                // it is lazy and out of current session
                MessageTemplate template = config.getAnnounceTemplate() != null
                        ? templateService.getById(config.getAnnounceTemplate().getId()) : null;
                templateService
                        .createMessage(template)
                        .withFallbackContent("discord.command.rank.levelup")
                        .withGuild(member.getGuild())
                        .withMember(member)
                        .withFallbackChannel(notifyChannel)
                        .withDirectAllowed(true)
                        .withVariable("level", result.getNewLevel())
                        .compileAndSend();
            }
            updateRewards(config, member, result.getRanking());
        }
    }

    private void giveCookie(LocalMember sender, LocalMember recipient, Date checkDate) {
        if (!cookieRepository.isFull(sender, recipient, checkDate)) {
            cookieRepository.save(new Cookie(sender, recipient));
            Ranking recipientRanking = configService.getRanking(recipient);
            if (recipientRanking != null) {
                recipientRanking.incrementCookies();
                rankingRepository.save(recipientRanking);
            }
        }
    }

    @Override
    @Transactional
    public void updateRewards(Member member) {
        RankingConfig config = configService.get(member.getGuild());
        if (config != null) {
            Ranking ranking = getRanking(member);
            updateRewards(config, member, ranking);
        }
    }

    private void updateRewards(RankingConfig config, Member member, Ranking ranking) {
        Member self = member.getGuild().getSelfMember();
        if (!discordService.isConnected(member.getGuild().getIdLong())
                || CollectionUtils.isEmpty(config.getRewards())
                || !self.hasPermission(Permission.MANAGE_ROLES)) {
            return;
        }

        int newLevel = RankingUtils.getLevelFromExp(ranking.getExp());

        List<RankingReward> rewards = config.getRewards().stream()
                .filter(e -> e.getRoleId() != null && e.getLevel() <= newLevel)
                .sorted(Comparator.comparing(RankingReward::getLevel))
                .collect(Collectors.toList());

        if (rewards.isEmpty()) {
            return;
        }
        RankingReward highest = rewards.remove(rewards.size() - 1);

        List<RankingReward> rewardsToAdd = new ArrayList<>();
        List<RankingReward> rewardsToRemove = new ArrayList<>();
        rewards.forEach(e -> (e.isReset() ? rewardsToRemove : rewardsToAdd).add(e));
        rewardsToAdd.add(highest);

        Set<Role> rolesToAdd = getRoles(member, rewardsToAdd);
        Set<Role> rolesToRemove = getRoles(member, rewardsToRemove);
        member.getGuild().modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();
    }

    private Set<Role> getRoles(Member member, List<RankingReward> rewards) {
        Member self = member.getGuild().getSelfMember();
        return rewards.stream()
                .map(RankingReward::getRoleId)                                          // map by id
                .map(roleId -> member.getGuild().getRoleById(roleId))            // find actual role object
                .filter(role -> role != null && self.canInteract(role) && !role.isManaged())          // check that we can assign that role
                .collect(Collectors.toSet());
    }

    private Ranking getRanking(Member member) {
        Ranking ranking = configService.getRanking(member);
        if (ranking == null) {
            LocalMember localMember = entityAccessor.getOrCreate(member);
            ranking = new Ranking();
            ranking.setMember(localMember);
            rankingRepository.save(ranking);
        }
        return ranking;
    }

    private Ranking getRanking(LocalMember member) {
        Ranking ranking = configService.getRanking(member);
        if (ranking == null) {
            ranking = new Ranking();
            member = memberService.get(member.getGuildId(), member.getUser().getUserId()); // force attach to transaction
            ranking.setMember(member);
            rankingRepository.save(ranking);
        }
        return ranking;
    }

    @Override
    @Transactional
    public RankingInfo getRankingInfo(Member member) {
        Ranking ranking = getRanking(member);
        RankingInfo rankingInfo = RankingUtils.calculateInfo(ranking);
        rankingInfo.setRank(rankingRepository.getRank(member.getGuild().getIdLong(), rankingInfo.getTotalExp()));
        return rankingInfo;
    }

    @Override
    @Transactional
    public RankingInfo getRankingInfo(LocalMember member) {
        Ranking ranking = getRanking(member);
        RankingInfo rankingInfo = RankingUtils.calculateInfo(ranking);
        rankingInfo.setRank(rankingRepository.getRank(member.getGuildId(), rankingInfo.getTotalExp()));
        return rankingInfo;
    }

    private boolean isIgnoredChannel(RankingConfig config, TextChannel channel) {
        if (channel == null || CollectionUtils.isEmpty(config.getIgnoredChannels())) {
            return false;
        }
        return config.getIgnoredChannels().contains(channel.getIdLong());
    }

    private static Date getCookieCoolDown() {
        return DateTime.now().minusMinutes(10).toDate();
    }
}
