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
package ru.caramel.juniperbot.core.feature.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.core.feature.model.FeatureProvider;
import ru.caramel.juniperbot.core.feature.model.FeatureSet;
import ru.caramel.juniperbot.core.common.persistence.LocalUser;
import ru.caramel.juniperbot.core.common.persistence.LocalUserRepository;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@FeatureProvider(priority = 1)
public class UserFeatureSetProvider extends BaseOwnerFeatureSetProvider {

    @Autowired
    private LocalUserRepository userRepository;

    private LoadingCache<Long, Set<FeatureSet>> userFeatureCache = CacheBuilder.newBuilder()
            .concurrencyLevel(7)
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(
                    new CacheLoader<>() {
                        public Set<FeatureSet> load(Long userId) {
                            String features = userRepository.findFeaturesByUserId(String.valueOf(userId));
                            return CommonUtils.safeEnumSet(features, FeatureSet.class);
                        }
                    });

    @Override
    @Transactional(readOnly = true)
    public Set<FeatureSet> getByUser(long userId) {
        try {
            return userFeatureCache.get(userId);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void applyFeatureSet(long userId, FeatureSet featureSet, boolean active) {
        LocalUser user = userRepository.findByUserId(String.valueOf(userId));
        if (user == null) {
            throw new IllegalStateException("No such user found");
        }
        Set<FeatureSet> featureSets = CommonUtils.safeEnumSet(user.getFeatures(), FeatureSet.class);
        if (active) {
            featureSets.add(featureSet);
        } else {
            featureSets.remove(featureSet);
        }
        user.setFeatures(CommonUtils.enumsString(featureSets));
        userRepository.save(user);
        userFeatureCache.invalidate(userId);
    }
}
