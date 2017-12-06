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

import lombok.Getter;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.audio.model.RepeatMode;
import ru.caramel.juniperbot.service.listeners.ReactionsListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MessageController {

    private enum Action {
        PLAY("▶"),
        PAUSE("\u23F8"),
        NEXT("⏭"),
        STOP("\u23F9"),
        REPEAT_CURRENT("\uD83D\uDD02"),
        REPEAT_ALL("\uD83D\uDD01"),
        REPEAT_NONE("➡"),
        VOLUME_DOWN("\uD83D\uDD09"),
        VOLUME_UP("\uD83D\uDD0A");

        private final String code;

        Action(String code) {
            this.code = code;
        }

        public static Action getForCode(String code) {
            return Stream.of(values()).filter(e -> Objects.equals(e.code, code)).findFirst().orElse(null);
        }
    }

    @Getter
    private final Message message;

    private final ReactionsListener reactionsListener;

    private final PlayerService playerService;

    private final AudioMessageManager messageManager;

    private boolean cancelled = false;

    private List<RequestFuture<Void>> reactionFutures = new ArrayList<>();

    public MessageController(ApplicationContext context, Message message) {
        this.message = message;
        this.reactionsListener = context.getBean(ReactionsListener.class);
        this.playerService = context.getBean(PlayerService.class);
        this.messageManager = context.getBean(AudioMessageManager.class);
        init();
    }

    private void init() {
        for (Action action : Action.values()) {
            try {
                reactionFutures.add(message.addReaction(action.code).submit());
            } catch (Exception ex) {
                // ignore
            }
        }

        reactionsListener.onReaction(message.getId(), event -> {
            if (!cancelled && !event.getUser().equals(event.getJDA().getSelfUser())) {
                String emote = event.getReaction().getEmote().getName();
                Action action = Action.getForCode(emote);
                if (action != null && handleAction(action)) {
                    event.getReaction().removeReaction(event.getUser()).submit();
                }
            }
            return false;
        });
    }

    private boolean handleAction(Action action) {
        PlaybackInstance instance = playerService.getInstance(message.getGuild());
        if (instance == null || !instance.isActive()) {
            return false;
        }

        boolean updateMessage = false;
        switch (action) {
            case PLAY:
                instance.resumeTrack(false);
                break;
            case PAUSE:
                instance.pauseTrack();
                break;
            case NEXT:
                playerService.skipTrack(message.getGuild());
                break;
            case STOP:
                messageManager.onMessage(message.getChannel(), instance.stop()
                        ? "discord.command.audio.stop" :"discord.command.audio.notStarted");
                break;
            case VOLUME_UP:
                if (instance.seekVolume(10, true)) {
                    updateMessage = true;
                }
                break;
            case VOLUME_DOWN:
                if (instance.seekVolume(10, false)) {
                    updateMessage = true;
                }
                break;
            case REPEAT_ALL:
                if (RepeatMode.ALL != instance.getMode()) {
                    updateMessage = true;
                }
                instance.setMode(RepeatMode.ALL);
                break;
            case REPEAT_NONE:
                if (RepeatMode.NONE != instance.getMode()) {
                    updateMessage = true;
                }
                instance.setMode(RepeatMode.NONE);
                break;
            case REPEAT_CURRENT:
                if (RepeatMode.CURRENT != instance.getMode()) {
                    updateMessage = true;
                }
                instance.setMode(RepeatMode.CURRENT);
                break;
        }
        if (updateMessage && instance.getCurrent() != null) {
            messageManager.updateMessage(instance.getCurrent());
        }
        return true;
    }

    public void remove(boolean soft) {
        if (soft) {
            cancelled = true;
            reactionFutures.forEach(e -> e.cancel(false));
            message.clearReactions().complete();
        } else {
            message.delete().complete();
        }
    }
}
