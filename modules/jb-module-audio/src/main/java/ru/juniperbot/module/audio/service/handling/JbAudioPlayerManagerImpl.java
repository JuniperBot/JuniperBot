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
package ru.juniperbot.module.audio.service.handling;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DecodedTrackHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JbAudioPlayerManagerImpl extends DefaultAudioPlayerManager implements JbAudioPlayerManager {

    @Value("${discord.audio.resamplingQuality:MEDIUM}")
    private AudioConfiguration.ResamplingQuality resamplingQuality;

    @Value("${discord.audio.frameBufferDuration:2000}")
    private int frameBufferDuration;

    @Value("${discord.audio.itemLoaderThreadPoolSize:500}")
    private int itemLoaderThreadPoolSize;

    @Autowired
    private List<AudioSourceManager> audioSourceManagers;

    @PostConstruct
    public void init() {
        getConfiguration().setResamplingQuality(resamplingQuality);
        setFrameBufferDuration(frameBufferDuration);
        setItemLoaderThreadPoolSize(itemLoaderThreadPoolSize);
        registerSourceManager(new YoutubeAudioSourceManager(true));
        registerSourceManager(new SoundCloudAudioSourceManager());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        if (CollectionUtils.isNotEmpty(audioSourceManagers)) {
            audioSourceManagers.forEach(this::registerSourceManager);
        }
    }

    @Override
    public byte[] encodeTrack(AudioTrack track) {
        if (track == null) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MessageOutput output = new MessageOutput(outputStream);
            encodeTrack(output, track);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.warn("Could not encode track {}", track);
        }
        return null;
    }

    @Override
    public AudioTrack decodeTrack(byte[] data) {
        if (ArrayUtils.isEmpty(data)) {
            return null;
        }
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            DecodedTrackHolder holder = decodeTrack(new MessageInput(stream));
            return holder != null && holder.decodedTrack != null ? holder.decodedTrack : null;
        } catch (IOException e) {
            log.warn("Could not decode track");
        }
        return null;
    }
}
