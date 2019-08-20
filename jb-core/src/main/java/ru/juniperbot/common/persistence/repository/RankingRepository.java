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
package ru.juniperbot.common.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.juniperbot.common.persistence.entity.LocalMember;
import ru.juniperbot.common.persistence.entity.Ranking;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    @Query("SELECT count(r) FROM Ranking r, LocalMember m WHERE r.member.id = m.id AND m.guildId = :guildId")
    long countByGuildId(@Param("guildId") long guildId);

    @Query("SELECT r FROM Ranking r, LocalMember m WHERE r.member.id = m.id AND m.guildId = :guildId AND (lower(m.effectiveName) like %:name% OR lower(m.user.name) like %:name%)")
    Page<Ranking> findByGuildId(@Param("guildId") long guildId, @Param("name") String name, Pageable pageable);

    @Query("SELECT r FROM Ranking r, LocalMember m WHERE r.member = m.id AND m.guildId = :guildId AND m.user.userId = :userId")
    Ranking findByGuildIdAndUserId(@Param("guildId") long guildId, @Param("userId") String userId);

    Ranking findByMember(LocalMember member);

    @Modifying
    @Query("UPDATE Ranking r SET r.exp = 0 WHERE r.member = :member")
    int resetMember(@Param("member") LocalMember member);

    @Modifying
    @Query("UPDATE Ranking r SET r.exp = 0 WHERE r.member IN (SELECT m FROM LocalMember m WHERE m.guildId = :guildId)")
    int resetAll(@Param("guildId") long guildId);

    @Modifying
    @Query("UPDATE Ranking r SET r.cookies = 0 WHERE r.member IN (SELECT m FROM LocalMember m WHERE m.guildId = :guildId)")
    int resetCookies(@Param("guildId") long guildId);

    @Query(value = "SELECT count(r.id) + 1 FROM ranking r JOIN member m ON m.id = r.member_id AND m.guild_id = ?1 WHERE r.exp > ?2", nativeQuery = true)
    long getRank(long guildId, long exp);

}
