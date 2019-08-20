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
package ru.juniperbot.common.worker.feature.provider;

import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.model.FeatureSet;
import ru.juniperbot.common.worker.feature.service.FeatureSetProvider;
import ru.juniperbot.common.worker.shared.service.DiscordService;

import java.util.Set;

public abstract class BaseOwnerFeatureSetProvider implements FeatureSetProvider {

    @Autowired
    private DiscordService discordService;

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailableForUser(long userId, FeatureSet featureSet) {
        return getByUser(userId).contains(featureSet);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(long guildId, FeatureSet featureSet) {
        Long ownerId = getOwnerId(guildId);
        return ownerId != null && isAvailableForUser(ownerId, featureSet);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<FeatureSet> getByGuild(long guildId) {
        Long ownerId = getOwnerId(guildId);
        return ownerId != null ? getByUser(ownerId) : Set.of();
    }

    private Long getOwnerId(long guildId) {
        if (!discordService.isConnected(guildId)) {
            return null;
        }
        Guild guild = discordService.getGuildById(guildId);
        return guild != null ? guild.getOwnerIdLong() : null;
    }
}
