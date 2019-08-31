/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.model.request;

import lombok.*;
import net.dv8tion.jda.api.entities.Member;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingUpdateRequest implements Serializable {

    private static final long serialVersionUID = -3799974593630579892L;

    private long guildId;

    @NotNull
    private String userId;

    private Integer level;

    private boolean resetCookies;

    private boolean resetVoiceActivity;

    public RankingUpdateRequest(@NonNull Member member) {
        guildId = member.getGuild().getIdLong();
        userId = member.getUser().getId();
    }
}
