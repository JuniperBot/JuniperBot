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
package ru.juniperbot.common.service;

import net.dv8tion.jda.api.entities.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.juniperbot.common.model.RankingInfo;
import ru.juniperbot.common.model.request.RankingUpdateRequest;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.Ranking;
import ru.juniperbot.common.persistence.entity.RankingConfig;

public interface RankingConfigService extends DomainService<RankingConfig> {

    boolean isEnabled(long guildId);

    long countRankings(long guildId);

    Integer getMaxVoiceMembers(long guildId);

    Page<RankingInfo> getRankingInfos(long guildId, String search, Pageable pageable);

    void update(RankingUpdateRequest request);

    void resetAll(long guildId, boolean levels, boolean cookies, boolean voiceActivity);

    boolean isBanned(RankingConfig config, Member member);

    Ranking getRanking(LocalMember member);

    Ranking getRanking(Member member);
}
