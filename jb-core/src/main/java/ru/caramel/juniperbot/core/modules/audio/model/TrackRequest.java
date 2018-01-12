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
package ru.caramel.juniperbot.core.modules.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

@Getter
@Setter
public class TrackRequest {

    private AudioTrack track;

    private final Member member;

    private final TextChannel channel;

    private boolean resetMessage;

    private boolean resetOnResume;

    public TrackRequest(AudioTrack track, Member member, TextChannel channel) {
        this.track = track;
        this.member = member;
        this.channel = channel;
    }

    public void reset() {
        if (track != null) {
            Object data = track.getUserData();
            track = track.makeClone();
            track.setUserData(data);
        }
    }

    public Guild getGuild() {
        if (channel != null) {
            return channel.getGuild();
        }
        if (member != null) {
            return member.getGuild();
        }
        return null;
    }
}
