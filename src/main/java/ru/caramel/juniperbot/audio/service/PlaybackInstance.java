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
package ru.caramel.juniperbot.audio.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.audio.model.TrackRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PlaybackInstance {

    private final AudioPlayer player;

    private final AudioManager audioManager;

    private final Guild guild;

    private final List<TrackRequest> playlist = Collections.synchronizedList(new ArrayList<>());

    private RepeatMode mode = RepeatMode.NONE;

    private int cursor = -1;

    private Long activeTime;

    public PlaybackInstance(AudioPlayer player, Guild guild) {
        this.player = player;
        this.guild = guild;
        audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(new GuildAudioSendHandler(player));
        activeTime = System.currentTimeMillis();
    }

    public synchronized void reset() {
        mode = RepeatMode.NONE;
        playlist.clear();
        player.playTrack(null);
        cursor = -1;
        if (isConnected()) {
            audioManager.closeAudioConnection();
        }
    }

    public synchronized void play(TrackRequest request) {
        offer(request);
        if (player.getPlayingTrack() == null) {
            mode = RepeatMode.NONE;
            cursor = 0;
            player.setPaused(false);
            player.playTrack(request.getTrack());
        }
    }

    public synchronized boolean playNext() {
        if (RepeatMode.CURRENT.equals(mode)) {
            getCurrent().reset();
            player.playTrack(getCurrent().getTrack());
            return true;
        }
        if (cursor < playlist.size() - 1) {
            cursor++;
            player.playTrack(getCurrent().getTrack());
            return true;
        }
        if (RepeatMode.ALL.equals(mode)) {
            cursor = 0;
            playlist.forEach(TrackRequest::reset);
            player.playTrack(getCurrent().getTrack());
            return true;
        }
        return false;
    }

    public synchronized boolean stop() {
        boolean active = isActive();
        if (active) {
            player.stopTrack();
        }
        return active;
    }

    public synchronized boolean pauseTrack() {
        boolean playing = isActive() && !player.isPaused();
        if (playing) {
            player.setPaused(true);
        }
        return playing;
    }

    public synchronized boolean resumeTrack() {
        boolean paused = isActive() && player.isPaused();
        if (paused) {
            player.setPaused(false);
        }
        return paused;
    }

    public synchronized boolean setMode(RepeatMode mode) {
        boolean result = isActive();
        if (result) {
            this.mode = mode;
        }
        return result;
    }

    public synchronized void openAudioConnection(VoiceChannel channel) {
        audioManager.openAudioConnection(channel);
    }

    public synchronized boolean isConnected() {
        return audioManager.isConnected() || audioManager.isAttemptingToConnect();
    }

    public synchronized boolean isActive() {
        return audioManager != null && isConnected()
                && audioManager.getConnectedChannel() != null && player.getPlayingTrack() != null;
    }

    public synchronized void setVolume(int volume) {
        player.setVolume(volume);
    }

    public synchronized boolean shuffle() {
        if (playlist.isEmpty()) {
            return false;
        }
        Collections.shuffle(getOnGoing());
        return true;
    }

    public synchronized TrackRequest getCurrent() {
        return cursor < 0 ? null : playlist.get(cursor);
    }

    private synchronized List<TrackRequest> getPast() {
        return cursor < 0 ? Collections.emptyList() : playlist.subList(0, cursor);
    }

    private synchronized List<TrackRequest> getOnGoing() {
        return cursor < 0 || cursor == playlist.size() - 1
                ? Collections.emptyList() : playlist.subList(cursor + 1, playlist.size());
    }

    public synchronized List<TrackRequest> getQueue() {
        List<TrackRequest> result = new ArrayList<>();
        if (getCurrent() != null) {
            result.add(getCurrent());
        }
        result.addAll(getOnGoing());
        return Collections.unmodifiableList(result);
    }

    public synchronized List<TrackRequest> getQueue(Member member) {
        return getQueue().stream().filter(e -> member.equals(e.getMember())).collect(Collectors.toList());
    }

    public synchronized boolean seek(long position) {
        if (isActive() && !player.getPlayingTrack().isSeekable()) {
            return false;
        }
        player.getPlayingTrack().setPosition(position);
        return true;
    }

    public synchronized void offer(TrackRequest request) {
        request.getTrack().setUserData(this);
        playlist.add(request);
    }
}
