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
package ru.caramel.juniperbot.module.audio.model;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

@Getter
@Setter
public class TrackRequest {

    private AudioTrack track;

    private final JDA jda;

    private final long guildId;

    private final long channelId;

    private final long memberId;

    private Long endMemberId;

    private boolean resetMessage;

    private boolean resetOnResume;

    private EndReason endReason;

    private Long timeCode;

    public TrackRequest(AudioTrack track, Member member, TextChannel channel) {
        this(track, member, channel, null);
    }

    public TrackRequest(AudioTrack track, Member member, TextChannel channel, Long timeCode) {
        this.jda = member.getJDA();
        this.track = track;
        this.guildId = member.getGuild().getIdLong();
        this.memberId = member.getUser().getIdLong();
        this.channelId = channel.getIdLong();
        this.timeCode = timeCode;
    }

    public void reset() {
        endReason = null;
        endMemberId = null;
        if (track != null) {
            Object data = track.getUserData();
            track = track.makeClone();
            track.setUserData(data);
        }
    }

    public TextChannel getChannel() {
        return jda.getTextChannelById(channelId);
    }

    public Member getMember() {
        Guild guild = jda.getGuildById(guildId);
        return guild != null ? guild.getMemberById(memberId) : null;
    }
}
