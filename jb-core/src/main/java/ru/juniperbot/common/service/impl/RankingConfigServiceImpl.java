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
package ru.juniperbot.common.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.repository.*;
import ru.juniperbot.common.service.RankingConfigService;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.persistence.entity.Ranking;
import ru.juniperbot.common.persistence.entity.RankingConfig;
import ru.juniperbot.common.utils.RankingUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class RankingConfigServiceImpl extends AbstractDomainServiceImpl<RankingConfig, RankingConfigRepository> implements RankingConfigService {

    @Autowired
    private LocalMemberRepository memberRepository;

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private CookieRepository cookieRepository;

    public RankingConfigServiceImpl(@Autowired RankingConfigRepository repository) {
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
        Ranking ranking = getRanking(member);
        if (ranking == null) {
            return null;
        }
        RankingInfo rankingInfo = RankingUtils.calculateInfo(ranking);
        rankingInfo.setRank(rankingRepository.getRank(member.getGuild().getIdLong(), rankingInfo.getTotalExp()));
        return rankingInfo;
    }

    @Override
    @Transactional(readOnly = true)
    public long countRankings(long guildId) {
        return rankingRepository.countByGuildId(guildId);
    }

    @Transactional
    @Override
    public Page<RankingInfo> getRankingInfos(long guildId, String search, Pageable pageable) {
        Page<Ranking> rankings = rankingRepository.findByGuildId(guildId, search != null ? search.toLowerCase() : "", pageable);
        AtomicLong rankCounter = new AtomicLong(pageable.getOffset());
        return rankings.map(e -> {
            RankingInfo info = RankingUtils.calculateInfo(e);
            info.setRank(StringUtils.isEmpty(search)
                    ? rankCounter.incrementAndGet()
                    : rankingRepository.getRank(guildId, info.getTotalExp()));
            return info;
        });
    }

    @Transactional
    @Override
    public void update(RankingUpdateRequest request) {
        LocalMember localMember = memberRepository.findByGuildIdAndUserId(request.getGuildId(), request.getUserId());
        Ranking ranking = getRanking(localMember);
        if (ranking == null) {
            return;
        }

        Integer level = request.getLevel();

        if (level != null) {
            if (level > RankingUtils.MAX_LEVEL) {
                level = RankingUtils.MAX_LEVEL;
            } else if (level < 0) {
                level = 0;
            }
            ranking.setExp(RankingUtils.getLevelTotalExp(level));
            rankingRepository.save(ranking);
        }

        if (request.isResetCookies()) {
            cookieRepository.deleteByRecipient(request.getGuildId(), request.getUserId());
            ranking.setCookies(0);
            rankingRepository.save(ranking);
        }
    }

    @Transactional
    @Override
    public void resetAll(long guildId, boolean levels, boolean cookies) {
        if (levels) {
            rankingRepository.resetAll(guildId);
        }
        if (cookies) {
            cookieRepository.deleteByGuild(guildId);
            rankingRepository.resetCookies(guildId);
        }
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

    @Override
    @Transactional
    public Ranking getRanking(LocalMember member) {
        Ranking ranking = rankingRepository.findByMember(member);
        if (ranking == null && member != null) {
            ranking = new Ranking();
            ranking.setMember(member);
            rankingRepository.save(ranking);
        }
        return ranking;
    }

    @Override
    @Transactional
    public Ranking getRanking(Member member) {
        return rankingRepository.findByMember(member);
    }

    @Override
    protected Class<RankingConfig> getDomainClass() {
        return RankingConfig.class;
    }
}
