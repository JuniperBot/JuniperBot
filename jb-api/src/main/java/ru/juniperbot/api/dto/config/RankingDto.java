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
package ru.juniperbot.api.dto.config;

import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.api.dto.MessageTemplateDto;
import ru.juniperbot.common.model.RankingReward;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RankingDto implements Serializable {
    private static final long serialVersionUID = 3035748702987797559L;

    private boolean enabled;

    private boolean announcementEnabled;

    @Valid
    private MessageTemplateDto announceTemplate;

    private boolean resetOnLeave;

    private String[] bannedRoles;

    private List<RankingReward> rewards;

    private Set<String> ignoredChannels;

    private boolean cookieEnabled;
}
