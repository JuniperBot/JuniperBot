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
package ru.caramel.juniperbot.core.support.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.feature.model.FeatureSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties("discord.support")
public class SupportConfiguration {

    private Long guildId;
    private Long donatorRoleId;
    private Map</* roleId*/ String, Set<FeatureSet>> featuredRoles = new HashMap<>();

}
