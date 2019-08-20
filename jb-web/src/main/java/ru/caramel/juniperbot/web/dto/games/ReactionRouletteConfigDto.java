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
package ru.caramel.juniperbot.web.dto.games;

import lombok.Getter;
import lombok.Setter;
import ru.juniperbot.common.model.discord.EmoteDto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class ReactionRouletteConfigDto implements Serializable {
    private static final long serialVersionUID = -7868033704887727170L;

    private ReactionRouletteDto config;

    private List<EmoteDto> emotes = Collections.emptyList();
}
