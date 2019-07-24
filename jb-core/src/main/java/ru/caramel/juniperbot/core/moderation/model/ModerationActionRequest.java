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
package ru.caramel.juniperbot.core.moderation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class ModerationActionRequest implements Serializable {
    private static final long serialVersionUID = 6374637369757626407L;

    @NonNull
    private ModerationActionType type;

    @NonNull
    private Member violator;

    private Member moderator;

    private String reason;

    private TextChannel channel;

    private boolean global;

    private Integer duration;

    @Builder.Default
    private boolean auditLogging = true;

    private boolean stateless;

    public Guild getGuild() {
        if (moderator != null) {
            return moderator.getGuild();
        }
        if (violator != null) {
            return violator.getGuild();
        }
        if (channel != null) {
            return channel.getGuild();
        }
        return null;
    }
}
