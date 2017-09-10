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
package ru.caramel.juniperbot.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import ru.caramel.juniperbot.audio.service.PlaybackInstance;
import ru.caramel.juniperbot.audio.model.TrackRequest;
import ru.caramel.juniperbot.audio.service.*;
import ru.caramel.juniperbot.commands.model.*;
import ru.caramel.juniperbot.integration.youtube.YouTubeClient;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;

import java.util.List;
import java.util.stream.Collectors;

@DiscordCommand(
        key = "discord.command.play.key",
        description = "discord.command.play.desc",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 104)
public class PlayCommand extends AudioCommand {

    protected static final String ATTR_SEARCH_MESSAGE = "search-message";

    protected static final String ATTR_SEARCH_RESULTS = "search-results";

    protected static final String ATTR_SEARCH_ACTIONS = "search-actions";

    @Autowired
    private YouTubeClient youTubeClient;

    @Autowired
    protected ValidationService validationService;

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String query) throws DiscordException {
        if (!message.getMessage().getAttachments().isEmpty()) {
            query = message.getMessage().getAttachments().get(0).getUrl();
        }

        List<String> results = (List<String>) context.getAttribute(ATTR_SEARCH_RESULTS);
        if (StringUtils.isNumeric(query) && CollectionUtils.isNotEmpty(results)) {
            int index = Integer.parseInt(query) - 1;
            query = getChoiceUrl(context, index);
            if (query == null) {
                messageManager.onQueueError(message.getChannel(), "discord.command.audio.play.select", results.size());
                return false;
            }
        }
        message.getTextChannel().sendTyping().queue();
        if (!ResourceUtils.isUrl(query)) {
            String result = youTubeClient.searchForUrl(query);
            query = result != null ? result : query;
        }
        Hibernate.initialize(context.getConfig().getMusicConfig());
        loadAndPlay(message.getTextChannel(), context, message.getMember(), query);
        return true;
    }

    @SuppressWarnings("unchecked")
    protected String getChoiceUrl(BotContext context, int index) {
        List<String> results = (List<String>) context.getAttribute(ATTR_SEARCH_RESULTS);
        if (index < 0 || CollectionUtils.isEmpty(results) || index > results.size() - 1) {
            return null;
        }
        List<RequestFuture<Void>> actions = (List<RequestFuture<Void>>) context.getAttribute(ATTR_SEARCH_ACTIONS);
        if (actions != null) {
            actions.forEach(e1 -> e1.cancel(true));
            context.removeAttribute(ATTR_SEARCH_ACTIONS);
        }
        context.removeAttribute(Message.class, ATTR_SEARCH_MESSAGE).delete().queue();
        return (String) context.removeAttribute(List.class, ATTR_SEARCH_RESULTS).get(index);
    }

    protected void loadAndPlay(final TextChannel channel, final BotContext context, final Member requestedBy, final String trackUrl) {
        PlaybackInstance instance = playerService.getInstance(channel.getGuild());
        playerService.getPlayerManager().loadItemOrdered(instance, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                try {
                    validationService.validateSingle(track, requestedBy, context);
                    playerService.play(new TrackRequest(track, requestedBy, channel));
                } catch (DiscordException e) {
                    messageManager.onQueueError(channel, e.getMessage(), e.getArgs());
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                try {
                    List<AudioTrack> tracks = validationService.filterPlaylist(playlist, requestedBy, context);
                    playerService.play(tracks.stream().map(e -> new TrackRequest(e , requestedBy, channel)).collect(Collectors.toList()));
                } catch (DiscordException e) {
                    messageManager.onQueueError(channel, e.getMessage(), e.getArgs());
                }
            }

            @Override
            public void noMatches() {
                messageManager.onNoMatches(channel, trackUrl);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                messageManager.onQueueError(channel, "discord.command.audio.error", e.getMessage());
            }
        });
    }
}
