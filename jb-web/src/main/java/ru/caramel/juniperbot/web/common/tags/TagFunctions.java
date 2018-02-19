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
package ru.caramel.juniperbot.web.common.tags;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import ru.caramel.juniperbot.module.ranking.model.Reward;
import ru.caramel.juniperbot.web.dto.RankingConfigDto;

import java.util.Locale;
import java.util.Objects;

public final class TagFunctions {

    private TagFunctions() {
    }

    public static Integer getLevelForRole(RankingConfigDto dto, Long roleId) {
        String roleString = String.valueOf(roleId);
        return CollectionUtils.isNotEmpty(dto.getRewards()) ? dto.getRewards().stream()
                .filter(e -> Objects.equals(roleString, e.getRoleId()))
                .map(Reward::getLevel).findFirst().orElse(null) : null;
    }

    public static String getDisplayLanguage() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String name = locale.getDisplayLanguage(locale);
        return StringUtils.capitalize(name);
    }
}
