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
package ru.caramel.juniperbot.core.feature.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import ru.caramel.juniperbot.core.feature.model.FeatureSet;

import java.util.Collections;
import java.util.Set;

public interface FeatureSetService {

    void sendBonusMessage(long channelId);

    void sendBonusMessage(long channelId, String title);

    boolean isAvailable(long guildId, FeatureSet featureSet);

    boolean isAvailableForUser(long userId, FeatureSet featureSet);

    Set<FeatureSet> getByGuild(long guildId);

    Set<FeatureSet> getByUser(long userId);

    default Set<FeatureSet> getAvailable(Guild guild) {
        return guild != null ? getByGuild(guild.getIdLong()) : Collections.emptySet();
    }

    default Set<FeatureSet> getAvailableByUser(User user) {
        return user != null ? getByUser(user.getIdLong()) : Collections.emptySet();
    }

    default boolean isAvailable(long guildId) {
        return isAvailable(guildId, FeatureSet.BONUS);
    }

    default boolean isAvailable(Guild guild) {
        return isAvailable(guild, FeatureSet.BONUS);
    }

    default boolean isAvailable(Guild guild, FeatureSet featureSet) {
        return guild != null && isAvailable(guild.getIdLong(), featureSet);
    }

    default boolean isAvailableForUser(long userId) {
        return isAvailableForUser(userId, FeatureSet.BONUS);
    }

    default boolean isAvailableForUser(User user) {
        return isAvailableForUser(user, FeatureSet.BONUS);
    }

    default boolean isAvailableForUser(User user, FeatureSet featureSet) {
        return user != null && isAvailableForUser(user.getIdLong(), featureSet);
    }
}
