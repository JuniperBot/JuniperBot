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
package ru.caramel.juniperbot.core.feature.persistence;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.core.common.persistence.base.UserEntity;
import ru.caramel.juniperbot.core.feature.model.FeatureSet;
import ru.caramel.juniperbot.core.utils.CommonUtils;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.Set;

@Getter
@Setter
@MappedSuperclass
public class FeaturedUserEntity extends UserEntity {
    private static final long serialVersionUID = -1439894653981742651L;

    @Column
    private String features;

    @Transient
    public Set<FeatureSet> getFeatureSets() {
        return CommonUtils.safeEnumSet(features, FeatureSet.class);
    }

    @Transient
    public void setFeatureSets(Set<FeatureSet> featureSets) {
        this.features = CommonUtils.enumsString(featureSets);
    }
}
