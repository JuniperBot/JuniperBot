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
package ru.caramel.juniperbot.module.steam.persistence.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.caramel.juniperbot.module.steam.persistence.entity.SteamApp;

import java.util.List;
import java.util.Set;

@Repository
public interface SteamAppRepository extends JpaRepository<SteamApp, Long> {

    @Query("SELECT app.appId FROM SteamApp app")
    Set<Long> findAllIds();

    @Modifying
    @Query("DELETE FROM SteamApp app WHERE app.appId in :ids")
    void deleteApps(@Param("ids") Set<Long> ids);

    @Query("SELECT app FROM SteamApp app WHERE fts('pg_catalog.english', app.terms, :query) = true ORDER BY fts_rank('pg_catalog.english', app.terms, :query, 2) DESC")
    List<SteamApp> search(@Param("query") String query, Pageable pageable);

    SteamApp findByAppId(Long appId);
}
