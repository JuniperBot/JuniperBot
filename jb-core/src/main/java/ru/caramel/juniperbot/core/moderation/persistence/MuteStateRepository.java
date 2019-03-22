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
package ru.caramel.juniperbot.core.moderation.persistence;

import net.dv8tion.jda.core.entities.Member;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.core.common.persistence.base.MemberRepository;

@Repository
public interface MuteStateRepository extends MemberRepository<MuteState> {

    void deleteByGuildIdAndUserIdAndChannelId(long guildId, String userId, String channelId);

    default void deleteByMember(Member member, String channelId) {
        deleteByGuildIdAndUserIdAndChannelId(member.getGuild().getIdLong(), member.getUser().getId(), channelId);
    }
}
