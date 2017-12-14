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
package ru.caramel.juniperbot.ranking.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import ru.caramel.juniperbot.persistence.entity.LocalMember;
import ru.caramel.juniperbot.persistence.entity.RankingConfig;
import ru.caramel.juniperbot.persistence.repository.LocalMemberRepository;
import ru.caramel.juniperbot.persistence.repository.RankingConfigRepository;
import ru.caramel.juniperbot.ranking.model.RankingInfo;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.MapPlaceholderResolver;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RankingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RankingService.class);

    private static PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}");

    @Autowired
    private LocalMemberRepository memberRepository;

    @Autowired
    private RankingConfigRepository rankingConfigRepository;

    @Autowired
    private MessageService messageService;

    private static Object DUMMY = new Object();

    private Cache<String, Object> coolDowns = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    private long getLevelExp(int level) {
        return 5 * (level * level) + (50 * level) + 100;
    }

    private int getLevelFromExp(long exp) {
        int level = 0;
        while (exp >= getLevelExp(level)) {
            exp -= getLevelExp(level);
            level++;
        }
        return level;
    }

    public boolean isBanned(RankingConfig config, Member member) {
        if (config.getBannedRoles() == null) {
            return false;
        }
        List<String> bannedRoles = Arrays.asList(config.getBannedRoles());
        return member.getRoles().stream()
                .anyMatch(e -> bannedRoles.contains(e.getName().toLowerCase()) || bannedRoles.contains(e.getId()));
    }

    public RankingInfo getRankingInfo(Member member) {
        if (!isApplicable(member)) {
            return null;
        }
        LocalMember localMember = getOrCreateMember(member);
        RankingInfo info = calculate(localMember);
        List<LocalMember> members = memberRepository.findByGuildIdOrderByExpDesc(member.getGuild().getId());
        info.setRank(members.indexOf(localMember) + 1);
        info.setTotalMembers(members.size());
        return info;
    }

    @Transactional(readOnly = true)
    public boolean checkExists(long serverId) {
        return memberRepository.countByGuildId(String.valueOf(serverId)) > 0;
    }

    @Transactional(readOnly = true)
    public List<RankingInfo> getRankingInfos(long serverId) {
        List<LocalMember> members = memberRepository.findByGuildIdOrderByExpDesc(String.valueOf(serverId));
        List<RankingInfo> result = new ArrayList<>(members.size());
        for (int i = 0; i < members.size(); i++) {
            RankingInfo info = calculate(members.get(i));
            info.setRank(i + 1);
            info.setTotalMembers(members.size());
            result.add(info);
        }
        return result;
    }

    @Transactional
    public LocalMember getOrCreateMember(Member member) {
        LocalMember localMember = memberRepository.findOneByGuildIdAndUserId(member.getGuild().getId(),
                member.getUser().getId());

        boolean shouldSave = false;
        if (localMember == null) {
            localMember = new LocalMember();
            localMember.setGuildId(member.getGuild().getId());
            localMember.setUserId(member.getUser().getId());
            shouldSave = true;
        }

        if (!Objects.equals(member.getUser().getName(), localMember.getName())) {
            localMember.setName(member.getUser().getName());
            shouldSave = true;
        }

        if (!Objects.equals(member.getUser().getDiscriminator(), localMember.getDiscriminator())) {
            localMember.setDiscriminator(member.getUser().getDiscriminator());
            shouldSave = true;
        }

        if (!Objects.equals(member.getEffectiveName(), localMember.getEffectiveName())) {
            localMember.setEffectiveName(member.getEffectiveName());
            shouldSave = true;
        }

        if (!Objects.equals(member.getUser().getAvatarUrl(), localMember.getAvatarUrl())) {
            localMember.setAvatarUrl(member.getUser().getAvatarUrl());
            shouldSave = true;
        }

        if (shouldSave) {
            memberRepository.save(localMember);
        }
        return localMember;
    }

    @Transactional
    public void onMessage(GuildMessageReceivedEvent event) {
        if (!isApplicable(event.getMember())) {
            return;
        }

        String memberKey = String.format("%s_%s", event.getGuild().getId(), event.getAuthor().getId());

        RankingConfig config = rankingConfigRepository.findByGuildId(event.getGuild().getIdLong());
        if (config == null || !config.isEnabled() || isBanned(config, event.getMember())
                || coolDowns.getIfPresent(memberKey) != null) {
            return;
        }

        LocalMember member = getOrCreateMember(event.getMember());

        int level = getLevelFromExp(member.getExp());

        member.setExp(member.getExp() + RandomUtils.nextLong(15, 25));
        memberRepository.save(member);
        coolDowns.put(memberKey, DUMMY);

        int newLevel = getLevelFromExp(member.getExp());
        if (newLevel == 1000) {
            return; // max level
        }
        if (level != newLevel) {
            if (config.isAnnouncementEnabled()) {
                MessageChannel channel = event.getChannel();
                String mention = event.getMember().getAsMention();
                if (config.isWhisper()) {
                    try {
                        channel = event.getAuthor().openPrivateChannel().complete();
                        mention = event.getAuthor().getAsMention();
                    } catch (Exception e) {
                        LOGGER.warn("Could not open private channel for {}", event.getAuthor(), e);
                    }
                }
                messageService.sendMessageSilent(channel::sendMessage, getAnnounce(config, mention, newLevel));
            }
            updateRewards(config, event.getMember(), member);
        }
    }

    @Transactional
    public void setLevel(long serverId, long userId, int level) {
        if (level > 1000) {
            level = 999;
        } else if (level < 0) {
            level = 0;
        }
        LocalMember localMember = memberRepository.findOneByGuildIdAndUserId(String.valueOf(serverId),
                String.valueOf(userId));
        if (localMember != null) {
            long exp = 0;
            for (int i = 0; i < level; i++) {
                exp += getLevelExp(i);
            }
            localMember.setExp(exp);
            memberRepository.save(localMember);
        }
    }

    private void updateRewards(RankingConfig config, Member member, LocalMember localMember) {
        Member self = member.getGuild().getSelfMember();
        if (CollectionUtils.isEmpty(config.getRewards())
                || !PermissionUtil.checkPermission(self, Permission.MANAGE_ROLES)) {
            return;
        }

        int newLevel = getLevelFromExp(localMember.getExp());
        Set<Role> rolesToGive = config.getRewards().entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() <= newLevel)  // filter by level
                .map(Map.Entry::getKey)                                       // map by id
                .filter(roleId -> member.getRoles().stream().noneMatch(role -> roleId.equals(role.getId()))) // filter by non-existent
                .map(roleId -> member.getGuild().getRoleById(roleId))         // find actual role object
                .filter(role -> role != null && self.canInteract(role))       // check that we can assign that role
                .collect(Collectors.toSet());

        if (!rolesToGive.isEmpty()) {
            member.getGuild().getController().addRolesToMember(member, rolesToGive).submit();
        }
    }

    private boolean isApplicable(Member member) {
        return member != null && !member.getGuild().getSelfMember().equals(member) && !member.getUser().isBot();
    }

    private RankingInfo calculate(LocalMember member) {
        RankingInfo info = new RankingInfo(member);
        info.setTotalExp(member.getExp());
        info.setLevel(getLevelFromExp(member.getExp()));
        long remaining = info.getTotalExp();
        for (int i = 0; i < info.getLevel(); i++) {
            remaining -= getLevelExp(i);
        }
        info.setRemainingExp(remaining);
        info.setLevelExp(getLevelExp(info.getLevel()));
        return info;
    }

    private String getAnnounce(RankingConfig config, String mention, int level) {
        MapPlaceholderResolver resolver = new MapPlaceholderResolver();
        resolver.put("user", mention);
        resolver.put("level", String.valueOf(level));
        String announce = config.getAnnouncement();
        if (StringUtils.isEmpty(announce)) {
            announce = messageService.getMessage("discord.command.rank.levelup");
        }
        return placeholderHelper.replacePlaceholders(announce, resolver);
    }
}
