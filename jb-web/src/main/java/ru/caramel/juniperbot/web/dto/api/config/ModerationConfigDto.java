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
package ru.caramel.juniperbot.web.dto.api.config;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.module.moderation.persistence.entity.ModerationConfig;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ModerationConfigDto implements Serializable {

    private static final long serialVersionUID = 2373520739258476656L;

    private List<Long> roles;

    private boolean publicColors;

    @Min(ModerationConfig.DEFAULT_MAX_WARNINGS)
    @Max(20)
    private int maxWarnings = ModerationConfig.DEFAULT_MAX_WARNINGS;
}
