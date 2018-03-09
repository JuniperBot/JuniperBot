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
package ru.caramel.juniperbot.module.audio.service;

import com.sedmelluq.discord.lavaplayer.player.event.*;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.IPlayer;
import lavalink.client.player.event.IPlayerEventListener;
import lavalink.client.player.event.PlayerEvent;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PlayerListenerAdapter implements AudioEventListener, IPlayerEventListener {

    private final Map<IPlayer, PlaybackInstance> instancesByPlayer = new ConcurrentHashMap<>();

    protected abstract void onTrackEnd(PlaybackInstance instance, AudioTrackEndReason endReason);

    protected abstract void onTrackStart(PlaybackInstance instance);

    protected abstract void onTrackException(PlaybackInstance instance, FriendlyException exception);

    protected void registerInstance(PlaybackInstance instance) {
        IPlayer player = instance.getPlayer();
        player.addListener(this);
        instancesByPlayer.put(player, instance);
    }

    protected void clearInstance(PlaybackInstance instance) {
        IPlayer player = instance.getPlayer();
        player.removeListener(this);
        instancesByPlayer.remove(player);
    }

    @Override
    public void onEvent(PlayerEvent event) {
        IPlayer player = event.getPlayer();
        PlaybackInstance instance = instancesByPlayer.get(player);

        if (event instanceof lavalink.client.player.event.TrackStartEvent) {
            if (instance != null) {
                onTrackStart(instance);
            }
        } else if (event instanceof lavalink.client.player.event.TrackEndEvent) {
            if (instance != null) {
                onTrackEnd(instance, ((lavalink.client.player.event.TrackEndEvent) event).getReason());
            }
        } else if (event instanceof lavalink.client.player.event.TrackExceptionEvent) {
            Exception e = ((lavalink.client.player.event.TrackExceptionEvent) event).getException();
            FriendlyException fe = e instanceof FriendlyException
                    ? (FriendlyException) e
                    : new FriendlyException("Unexpected exception", FriendlyException.Severity.SUSPICIOUS, e);
            onTrackException(instance, fe);
        }
    }

    @Override
    public void onEvent(AudioEvent event) {
        if (event instanceof TrackStartEvent) {
            AudioTrack track = ((TrackStartEvent) event).track;
            if (track != null) {
                PlaybackInstance instance = track.getUserData(PlaybackInstance.class);
                if (instance != null) {
                    onTrackStart(instance);
                }
            }
        } else if (event instanceof TrackEndEvent) {
            AudioTrack track = ((TrackEndEvent) event).track;
            if (track != null) {
                PlaybackInstance instance = track.getUserData(PlaybackInstance.class);
                if (instance != null) {
                    onTrackEnd(instance, ((TrackEndEvent) event).endReason);
                }
            }
        } else if (event instanceof TrackExceptionEvent) {
            AudioTrack track = ((TrackExceptionEvent) event).track;
            if (track != null) {
                onTrackException(track.getUserData(PlaybackInstance.class), ((TrackExceptionEvent) event).exception);
            }
        }
    }
}
