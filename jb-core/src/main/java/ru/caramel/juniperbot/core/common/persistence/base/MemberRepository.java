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
package ru.caramel.juniperbot.core.common.persistence.base;

import net.dv8tion.jda.core.entities.Member;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface MemberRepository<T extends MemberEntity> extends GuildRepository<T> {

    List<T> findAllByGuildIdAndUserId(long guildId, String userId);

    T findByGuildIdAndUserId(long guildId, String userId);

    void deleteByGuildIdAndUserId(long guildId, String userId);

    default List<T> findAllByMember(Member member) {
        return findAllByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId());
    }

    default T findByMember(Member member) {
        return findByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId());
    }

    default void deleteByMember(Member member) {
        deleteByGuildIdAndUserId(member.getGuild().getIdLong(), member.getUser().getId());
    }
}
