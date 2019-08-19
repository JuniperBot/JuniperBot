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
package ru.juniperbot.common.persistence.repository.base;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import ru.juniperbot.common.persistence.entity.base.FeaturedUserEntity;
import ru.juniperbot.common.persistence.repository.base.UserRepository;

@NoRepositoryBean
public interface FeaturedUserRepository<T extends FeaturedUserEntity> extends UserRepository<T> {

    @Query("SELECT u.features FROM #{#entityName} u WHERE u.userId = :userId")
    String findFeaturesByUserId(@Param("userId") String userId);
}
