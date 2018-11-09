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
package ru.caramel.juniperbot.module.ranking.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.persistence.repository.LocalMemberRepository;
import ru.caramel.juniperbot.core.service.*;
import ru.caramel.juniperbot.core.service.impl.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.core.utils.MapPlaceholderResolver;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.model.Reward;
import ru.caramel.juniperbot.module.ranking.persistence.entity.Cookie;
import ru.caramel.juniperbot.module.ranking.persistence.entity.Ranking;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.module.ranking.persistence.repository.CookieRepository;
import ru.caramel.juniperbot.module.ranking.persistence.repository.RankingConfigRepository;
import ru.caramel.juniperbot.module.ranking.persistence.repository.RankingRepository;
import ru.caramel.juniperbot.module.ranking.utils.RankingUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RankingServiceImpl extends AbstractDomainServiceImpl<RankingConfig, RankingConfigRepository> implements RankingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RankingServiceImpl.class);

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    private LocalMemberRepository memberRepository;

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private CookieRepository cookieRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private ContextService contextService;

    private static Object DUMMY = new Object();

    private Cache<String, Object> coolDowns = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    private final Set<Long> calculateQueue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RankingServiceImpl(@Autowired RankingConfigRepository repository) {
        super(repository, true);
    }

    @Override
    protected RankingConfig createNew(long guildId) {
        return new RankingConfig(guildId);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isEnabled(long guildId) {
        return repository.isEnabled(guildId);
    }

    @Transactional
    @Override
    public RankingInfo getRankingInfo(Member member) {
        return RankingUtils.calculateInfo(getRanking(member));
    }

    @Override
    public long countRankings(long guildId) {
        return rankingRepository.countByGuildId(guildId);
    }

    @Transactional
    @Override
    public Page<RankingInfo> getRankingInfos(long guildId, String search, Pageable pageable) {
        Page<Ranking> rankings = rankingRepository.findByGuildId(guildId, search != null ? search.toLowerCase() : "", pageable);
        return rankings.map(RankingUtils::calculateInfo);
    }

    @Transactional
    @Override
    public void onMessage(GuildMessageReceivedEvent event) {
        Guild guild = event.getGuild();
        RankingConfig config = get(guild);
        if (config == null || !memberService.isApplicable(event.getMember()) || !config.isEnabled() || isBanned(config, event.getMember())) {
            return;
        }

        String memberKey = String.format("%s_%s", guild.getId(), event.getAuthor().getId());
        if (coolDowns.getIfPresent(memberKey) == null && !isIgnoredChannel(config, event.getChannel())) {
            contextService.withContextAsync(guild, () -> {
                Ranking ranking = getRanking(event.getMember());
                int level = RankingUtils.getLevelFromExp(ranking.getExp());
                ranking.setExp(ranking.getExp() + RandomUtils.nextLong(15, 25));
                rankingRepository.save(ranking);
                calculateQueue.add(guild.getIdLong());
                coolDowns.put(memberKey, DUMMY);

                int newLevel = RankingUtils.getLevelFromExp(ranking.getExp());
                if (newLevel < 1000 && level != newLevel) {
                    if (config.isAnnouncementEnabled()) {
                        if (config.isWhisper()) {
                            try {
                                contextService.queue(guild, event.getAuthor().openPrivateChannel(), c -> {
                                    String content = getAnnounce(config, event.getAuthor().getAsMention(), newLevel);
                                    sendAnnounce(config, c, content);
                                });
                            } catch (Exception e) {
                                LOGGER.warn("Could not open private channel for {}", event.getAuthor(), e);
                            }
                        } else {
                            MessageChannel channel = config.getAnnouncementChannelId() != null ?
                                    guild.getTextChannelById(config.getAnnouncementChannelId()) : null;
                            String content = getAnnounce(config, event.getMember().getAsMention(), newLevel);
                            sendAnnounce(config, channel != null ? channel : event.getChannel(), content);
                        }
                    }
                    updateRewards(config, event.getMember(), ranking);
                }
            });
        }

        if (config.isCookieEnabled()
                && CollectionUtils.isNotEmpty(event.getMessage().getMentionedUsers())
                && StringUtils.isNotEmpty(event.getMessage().getContentRaw())
                && event.getMessage().getContentRaw().contains(RankingService.COOKIE_EMOTE)) {
            contextService.withContextAsync(null, () -> {
                Date checkDate = getCookieCoolDown();
                for (User user : event.getMessage().getMentionedUsers()) {
                    if (!user.isBot() && !Objects.equals(user, event.getAuthor())) {
                        Member recipientMember = guild.getMember(user);
                        if (recipientMember != null && memberService.isApplicable(recipientMember)) {
                            LocalMember sender = memberService.getOrCreate(event.getMember());
                            LocalMember recipient = memberService.getOrCreate(recipientMember);
                            giveCookie(sender, recipient, checkDate);
                        }
                    }
                }
            });
        }
    }

    private void sendAnnounce(RankingConfig config, MessageChannel channel, String content) {
        if (config.isEmbed()) {
            messageService.onEmbedMessage(channel, content);
        } else {
            messageService.sendMessageSilent(channel::sendMessage, content);
        }
    }

    @Transactional
    @Override
    public void giveCookie(Member senderMember, Member recipientMember) {
        if (!memberService.isApplicable(senderMember) || !memberService.isApplicable(recipientMember)) {
            return;
        }
        RankingConfig config = get(senderMember.getGuild());
        if (config != null && config.isCookieEnabled()) {
            LocalMember recipient = memberService.getOrCreate(recipientMember);
            LocalMember sender = memberService.getOrCreate(senderMember);
            giveCookie(sender, recipient, getCookieCoolDown());
        }
    }

    private void giveCookie(LocalMember sender, LocalMember recipient, Date checkDate) {
        if (!cookieRepository.isFull(sender, recipient, checkDate)) {
            inTransaction(() -> {
                cookieRepository.save(new Cookie(sender, recipient));
                Ranking recipientRanking = getRanking(recipient);
                if (recipientRanking != null) {
                    recipientRanking.incrementCookies();
                    rankingRepository.save(recipientRanking);
                }
            });
        }
    }

    @Transactional
    @Override
    public void setLevel(long guildId, String userId, int level) {
        Objects.requireNonNull(userId);
        if (level > RankingUtils.MAX_LEVEL) {
            level = RankingUtils.MAX_LEVEL;
        } else if (level < 0) {
            level = 0;
        }
        LocalMember localMember = memberRepository.findByGuildIdAndUserId(guildId, userId);

        Ranking ranking = getRanking(localMember);
        if (ranking != null) {
            ranking.setExp(RankingUtils.getLevelTotalExp(level));
            rankingRepository.save(ranking);
            rankingRepository.recalculateRank(guildId);
            if (discordService.isConnected(guildId)) {
                Guild guild = discordService.getShardManager().getGuildById(guildId);
                if (guild != null) {
                    Member member = guild.getMemberById(userId);
                    if (member != null) {
                        RankingConfig config = getByGuildId(guildId);
                        updateRewards(config, member, ranking);
                    }
                }
            }
        }
    }

    @Transactional
    @Override
    public void resetAll(long guildId) {
        rankingRepository.resetAll(guildId);
        rankingRepository.recalculateRank(guildId);
    }

    private void updateRewards(RankingConfig config, Member member, Ranking ranking) {
        Member self = member.getGuild().getSelfMember();
        if (!discordService.isConnected(member.getGuild().getIdLong())
                || CollectionUtils.isEmpty(config.getRewards())
                || !self.hasPermission(Permission.MANAGE_ROLES)) {
            return;
        }

        int newLevel = RankingUtils.getLevelFromExp(ranking.getExp());

        List<Reward> rewards = config.getRewards().stream()
                .filter(e -> e.getRoleId() != null && e.getLevel() <= newLevel)
                .sorted(Comparator.comparing(Reward::getLevel))
                .collect(Collectors.toList());

        if (rewards.isEmpty()) {
            return;
        }
        Reward highest = rewards.remove(rewards.size() - 1);

        List<Reward> rewardsToAdd = new ArrayList<>();
        List<Reward> rewardsToRemove = new ArrayList<>();
        rewards.forEach(e -> (e.isReset() ? rewardsToRemove : rewardsToAdd).add(e));
        rewardsToAdd.add(highest);

        Set<Role> rolesToAdd = getRoles(member, rewardsToAdd);
        Set<Role> rolesToRemove = getRoles(member, rewardsToRemove);
        member.getGuild().getController().modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();
    }

    private Set<Role> getRoles(Member member, List<Reward> rewards) {
        Member self = member.getGuild().getSelfMember();
        return rewards.stream()
                .map(Reward::getRoleId)                                          // map by id
                .map(roleId -> member.getGuild().getRoleById(roleId))            // find actual role object
                .filter(role -> role != null && self.canInteract(role) && !role.isManaged())          // check that we can assign that role
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isBanned(RankingConfig config, Member member) {
        if (config.getBannedRoles() == null) {
            return false;
        }
        List<String> bannedRoles = Arrays.asList(config.getBannedRoles());
        return CollectionUtils.isNotEmpty(member.getRoles()) && member.getRoles().stream()
                .anyMatch(e -> bannedRoles.contains(e.getName().toLowerCase()) || bannedRoles.contains(e.getId()));
    }

    private boolean isIgnoredChannel(RankingConfig config, TextChannel channel) {
        if (channel == null || CollectionUtils.isEmpty(config.getIgnoredChannels())) {
            return false;
        }
        return config.getIgnoredChannels().contains(channel.getIdLong());
    }

    private String getAnnounce(RankingConfig config, String mention, int level) {
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("user", mention);
        resolver.put("level", String.valueOf(level));
        String announce = config.getAnnouncement();
        if (StringUtils.isBlank(announce)) {
            announce = messageService.getMessage("discord.command.rank.levelup");
        }
        return placeholderHelper.replacePlaceholders(announce, resolver);
    }

    @Transactional
    public Ranking getRanking(LocalMember member) {
        Ranking ranking = rankingRepository.findByMember(member);
        if (ranking == null && member != null) {
            ranking = new Ranking();
            ranking.setMember(member);
            ranking.setRank(rankingRepository.countByGuildId(member.getGuildId()) + 1);
            rankingRepository.save(ranking);
        }
        return ranking;
    }

    @Transactional
    public Ranking getRanking(Member member) {
        Ranking ranking = rankingRepository.findByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId());
        if (ranking == null) {
            LocalMember localMember = memberService.getOrCreate(member);
            ranking = new Ranking();
            ranking.setMember(localMember);
            ranking.setRank(rankingRepository.countByGuildId(member.getGuild().getIdLong()) + 1);
            rankingRepository.save(ranking);
        }
        return ranking;
    }

    @Scheduled(fixedDelay = 300000)
    @Override
    public void calculateQueue() {
        synchronized (calculateQueue) {
            Set<Long> queue = new HashSet<>(calculateQueue);
            calculateQueue.clear();
            queue.forEach(rankingRepository::recalculateRank);
        }
    }

    private static Date getCookieCoolDown() {
        return DateTime.now().minusMinutes(10).toDate();
    }

    @Override
    protected Class<RankingConfig> getDomainClass() {
        return RankingConfig.class;
    }
}
