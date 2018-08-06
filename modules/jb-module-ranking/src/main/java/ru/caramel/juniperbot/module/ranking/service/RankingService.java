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

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.caramel.juniperbot.core.persistence.entity.LocalMember;
import ru.caramel.juniperbot.core.service.DomainService;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.persistence.entity.RankingConfig;

public interface RankingService extends DomainService<RankingConfig> {

    String COOKIE_EMOTE = "\uD83C\uDF6A";

    void onMessage(GuildMessageReceivedEvent event);

    boolean isEnabled(long guildId);

    RankingInfo getRankingInfo(Member member);

    long countRankings(long guildId);

    Page<RankingInfo> getRankingInfos(long guildId, String search, Pageable pageable);

    void setLevel(long guildId, String userId, int level);

    void resetAll(long guildId);

    boolean isBanned(RankingConfig config, Member member);

    void calculateQueue();

    void giveCookie(LocalMember sender, LocalMember recipient);

    void giveCookie(Member senderMember, Member recipientMember);
}
