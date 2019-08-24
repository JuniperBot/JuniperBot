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
package ru.juniperbot.module.audio.model;

import lavalink.client.player.IPlayer;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PlaybackInstance {

    private final IPlayer player;

    private final List<TrackRequest> playlist = Collections.synchronizedList(new ArrayList<>());

    private AudioManager audioManager;

    private RepeatMode mode;

    private int cursor;

    private long guildId;

    private Long playlistId;

    private String playlistUuid;

    private Long activeTime;

    public PlaybackInstance(long guildId, IPlayer player) {
        this.guildId = guildId;
        this.player = player;
        reset();
    }

    public synchronized void reset() {
        mode = RepeatMode.NONE;
        playlist.clear();
        player.stopTrack();
        cursor = -1;
        tick();
    }

    public synchronized void tick() {
        activeTime = System.currentTimeMillis();
    }

    public synchronized void play(TrackRequest request) {
        tick();
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

    public synchronized void stop() {
        player.stopTrack();
        reset();
    }

    public synchronized TrackRequest removeByIndex(int index) {
        if (index >= playlist.size() || index == cursor) {
            return null;
        }
        TrackRequest request = playlist.remove(index);
        if (index < cursor) {
            cursor--;
        }
        return request;
    }

    public synchronized boolean pauseTrack() {
        tick();
        boolean playing = !player.isPaused();
        if (playing) {
            player.setPaused(true);
        }
        return playing;
    }

    public synchronized boolean resumeTrack(boolean resetMessage) {
        tick();
        TrackRequest current = getCurrent();
        if (current != null) {
            current.setResetOnResume(resetMessage);
        }
        boolean paused = player.isPaused();
        if (paused) {
            player.setPaused(false);
        }
        return paused;
    }

    public synchronized void setMode(RepeatMode mode) {
        this.mode = mode;
    }

    public synchronized void setVolume(int volume) {
        player.setVolume(volume);
    }

    public synchronized boolean seekVolume(int amount, boolean up) {
        int currentValue = player.getVolume();
        if ((up && currentValue == 150) || (!up && currentValue == 0)) {
            return false;
        }
        int newValue = currentValue + (amount * (up ? 1 : -1));
        if (newValue < 0) {
            newValue = 0;
        }
        if (newValue > 150) {
            newValue = 150;
        }
        player.setVolume(newValue);
        return true;
    }

    public synchronized boolean shuffle() {
        if (playlist.isEmpty()) {
            return false;
        }
        Collections.shuffle(getOnGoing());
        return true;
    }

    public synchronized TrackRequest getCurrent() {
        return cursor < 0 || playlist.isEmpty() ? null : playlist.get(cursor);
    }

    private synchronized List<TrackRequest> getPast() {
        return cursor < 0 || playlist.isEmpty() ? Collections.emptyList() : playlist.subList(0, cursor);
    }

    private synchronized List<TrackRequest> getOnGoing() {
        return cursor < 0 || playlist.isEmpty() || cursor == playlist.size() - 1
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
        long memberId = member.getUser().getIdLong();
        return getQueue().stream().filter(e -> memberId == e.getMemberId()).collect(Collectors.toList());
    }

    public synchronized boolean seek(long position) {
        if (!player.getPlayingTrack().isSeekable()) {
            return false;
        }
        if (player.getPlayingTrack() != null) {
            player.seekTo(position);
        }
        return true;
    }

    public synchronized long getPosition() {
        try {
            long position = player.getTrackPosition();
            return position >= 0 ? position : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public synchronized void offer(TrackRequest request) {
        TrackData.setInstance(request.getTrack(), this);
        playlist.add(request);
    }
}
