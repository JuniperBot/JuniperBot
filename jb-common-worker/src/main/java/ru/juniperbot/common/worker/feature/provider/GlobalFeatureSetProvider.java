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

import org.springframework.beans.factory.annotation.Value;
import ru.juniperbot.common.model.FeatureSet;
import ru.juniperbot.common.worker.feature.service.FeatureSetProvider;

import java.util.Set;

@FeatureProvider(priority = 0)
public class GlobalFeatureSetProvider implements FeatureSetProvider {

    @Value("${bonus.global:false}")
    private boolean globalAccess;

    public void setGlobalAccess(boolean globalAccess) {
        this.globalAccess = globalAccess;
    }

    @Override
    public boolean isAvailable(long guildId, FeatureSet featureSet) {
        return globalAccess;
    }

    @Override
    public boolean isAvailableForUser(long userId, FeatureSet featureSet) {
        return globalAccess;
    }

    @Override
    public Set<FeatureSet> getByGuild(long guildId) {
        return globalAccess ? Set.of(FeatureSet.values()) : Set.of();
    }

    @Override
    public Set<FeatureSet> getByUser(long userId) {
        return globalAccess ? Set.of(FeatureSet.values()) : Set.of();
    }
}
