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
package ru.caramel.juniperbot.module.audio.utils;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.requests.RequestFuture;
import org.springframework.context.ApplicationContext;
import ru.caramel.juniperbot.core.listeners.ReactionsListener;
import ru.caramel.juniperbot.core.service.CommandsService;
import ru.caramel.juniperbot.core.service.ContextService;
import ru.caramel.juniperbot.core.service.MessageService;
import ru.caramel.juniperbot.module.audio.commands.PlayCommand;
import ru.caramel.juniperbot.module.audio.commands.control.PauseCommand;
import ru.caramel.juniperbot.module.audio.commands.control.RepeatCommand;
import ru.caramel.juniperbot.module.audio.commands.control.StopCommand;
import ru.caramel.juniperbot.module.audio.commands.control.VolumeCommand;
import ru.caramel.juniperbot.module.audio.commands.queue.SkipCommand;
import ru.caramel.juniperbot.module.audio.model.PlaybackInstance;
import ru.caramel.juniperbot.module.audio.model.RepeatMode;
import ru.caramel.juniperbot.module.audio.service.MusicConfigService;
import ru.caramel.juniperbot.module.audio.service.PlayerService;
import ru.caramel.juniperbot.module.audio.service.helper.AudioMessageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageController {

    @Getter
    private enum Action {
        PLAY(PlayCommand.KEY, "▶"),
        PAUSE(PauseCommand.KEY, "\u23F8"),
        NEXT(SkipCommand.KEY, "⏭"),
        STOP(StopCommand.KEY, "\u23F9"),
        REPEAT_CURRENT(RepeatCommand.KEY, "\uD83D\uDD02"),
        REPEAT_ALL(RepeatCommand.KEY, "\uD83D\uDD01"),
        REPEAT_NONE(RepeatCommand.KEY, "➡"),
        VOLUME_DOWN(VolumeCommand.KEY, "\uD83D\uDD09"),
        VOLUME_UP(VolumeCommand.KEY, "\uD83D\uDD0A");

        private final String code;

        private final String commandKey;

        Action(String commandKey, String code) {
            this.code = code;
            this.commandKey = commandKey;
        }

        public static Action getForCode(String code) {
            return Stream.of(values()).filter(e -> Objects.equals(e.code, code)).findFirst().orElse(null);
        }
    }

    private final JDA jda;

    private final long messageId;

    private final long channelId;

    private final long guildId;

    private final ReactionsListener reactionsListener;

    private final PlayerService playerService;

    private final AudioMessageManager messageManager;

    private final ContextService contextService;

    private final MessageService messageService;

    private final MusicConfigService musicConfigService;

    private final CommandsService commandsService;

    private boolean cancelled = false;

    private List<RequestFuture<Void>> reactionFutures = new ArrayList<>();

    public MessageController(ApplicationContext context, Message message) {
        this.jda = message.getJDA();
        this.messageId = message.getIdLong();
        this.channelId = message.getTextChannel().getIdLong();
        this.guildId = message.getGuild().getIdLong();
        this.reactionsListener = context.getBean(ReactionsListener.class);
        this.playerService = context.getBean(PlayerService.class);
        this.messageManager = context.getBean(AudioMessageManager.class);
        this.contextService = context.getBean(ContextService.class);
        this.musicConfigService = context.getBean(MusicConfigService.class);
        this.commandsService = context.getBean(CommandsService.class);
        this.messageService = context.getBean(MessageService.class);
        init(message);
    }

    private void init(Message message) {
        if (message.getGuild().getSelfMember().hasPermission(message.getTextChannel(),
                Permission.MESSAGE_MANAGE,
                Permission.MESSAGE_ADD_REACTION)) {
            for (Action action : getAvailableActions()) {
                try {
                    reactionFutures.add(message.addReaction(action.code).submit());
                } catch (Exception ex) {
                    // ignore
                }
            }

            reactionsListener.onReactionAdd(message.getId(), event -> {
                if (!cancelled && !event.getUser().equals(event.getJDA().getSelfUser())) {
                    String emote = event.getReaction().getReactionEmote().getName();
                    Action action = Action.getForCode(emote);
                    if (action != null) {
                        contextService.withContext(event.getGuild(), () -> handleAction(action, event.getMember()));
                    }
                    if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(),
                            Permission.MESSAGE_MANAGE)) {
                        event.getReaction().removeReaction(event.getUser()).queue();
                    }
                }
                return false;
            });
        }
    }

    private void handleAction(Action action, Member member) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null
                || !musicConfigService.hasAccess(member)
                || !playerService.isInChannel(member)
                || !playerService.isActive(guild)
                || !isAvailable(action, member)) {
            return;
        }
        PlaybackInstance instance = playerService.getInstance(guild);

        boolean updateMessage = false;
        switch (action) {
            case PLAY:
                playerService.resume(guild, false);
                break;
            case PAUSE:
                playerService.pause(guild);
                break;
            case NEXT:
                playerService.skipTrack(member, guild);
                break;
            case STOP:
                TextChannel channel = jda.getTextChannelById(channelId);
                if (playerService.stop(member, guild)) {
                    if (member != null) {
                        messageManager.onMessage(channel, "discord.command.audio.stop.member", member.getEffectiveName());
                    } else {
                        messageManager.onMessage(channel, "discord.command.audio.stop");
                    }
                } else {
                    messageManager.onMessage(channel, "discord.command.audio.notStarted");
                }
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
    }

    public void remove(boolean soft) {
        doForMessage(message -> {
            try {
                if (soft) {
                    cancelled = true;
                    if (message.getGuild().isAvailable() && message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                        reactionFutures.forEach(e -> e.cancel(false));
                        message.clearReactions().queue(e -> reactionsListener.unsubscribe(message.getId()));
                    }
                } else {
                    messageService.delete(message);
                    reactionsListener.unsubscribe(message.getId());
                }
            } catch (ErrorResponseException e) {
                switch (e.getErrorResponse()) {
                    case MISSING_ACCESS:
                        return;
                    default:
                        throw e;
                }
            }
        });
    }

    private List<Action> getAvailableActions() {
        return Stream.of(Action.values())
                .filter(e -> isAvailable(e, null))
                .collect(Collectors.toList());
    }

    private boolean isAvailable(Action action, Member member) {
        TextChannel channel = jda.getTextChannelById(channelId);
        return channel != null && !commandsService.isRestricted(action.commandKey, channel, member);
    }

    public void doForMessage(Consumer<Message> success) {
        doForMessage(success, null);
    }

    public void doForMessage(Consumer<? super Message> success, Consumer<? super Throwable> error) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.getMessageById(messageId).queue(
                    m -> contextService.withContext(guildId, () -> {
                        if (success != null) {
                            success.accept(m);
                        }
                    }),
                    t -> contextService.withContext(guildId, () -> {
                        if (error != null) {
                            error.accept(t);
                        }
                    }));
        }
    }
}
