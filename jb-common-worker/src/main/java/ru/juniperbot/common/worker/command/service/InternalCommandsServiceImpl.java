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
package ru.juniperbot.common.worker.command.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import ru.juniperbot.common.model.exception.DiscordException;
import ru.juniperbot.common.model.exception.ValidationException;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.entity.GuildConfig;
import ru.juniperbot.common.service.CommandConfigService;
import ru.juniperbot.common.worker.command.model.BotContext;
import ru.juniperbot.common.worker.command.model.Command;
import ru.juniperbot.common.worker.metrics.service.DiscordMetricsRegistry;
import ru.juniperbot.common.worker.metrics.service.StatisticsService;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Order(0)
@Service
public class InternalCommandsServiceImpl extends BaseCommandsService implements InternalCommandsService {

    @Autowired
    private CommandConfigService commandConfigService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private DiscordMetricsRegistry registry;

    private Cache<Long, BotContext> contexts = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();

    private Meter executions;

    private Counter counter;

    @PostConstruct
    public void init() {
        executions = statisticsService.getMeter(EXECUTIONS_METER);
        counter = statisticsService.getCounter(EXECUTIONS_COUNTER);
    }

    @Override
    public boolean isValidKey(GuildMessageReceivedEvent event, String key) {
        return holderService.isAnyCommand(key);
    }

    @Override
    public boolean sendCommand(GuildMessageReceivedEvent event, String content, String key, GuildConfig guildConfig) {
        TextChannel channel = event.getChannel();
        String locale = guildConfig != null ? guildConfig.getCommandLocale() : null;
        Command command = holderService.getByLocale(key, locale);
        if (command == null) {
            return false;
        }
        String rawKey = command.getKey();
        if (rawKey == null) {
            return false;
        }

        CommandConfig commandConfig = event.getGuild() != null ? commandConfigService.findByKey(event.getGuild().getIdLong(), rawKey) : null;
        if (!isApplicable(command, commandConfig, event.getAuthor(), event.getMember(), channel)) {
            return false;
        }

        if (commandConfig != null && isRestricted(event, commandConfig)) {
            return true;
        }
        Permission[] permissions = command.getPermissions();
        if (permissions != null && permissions.length > 0) {
            Member self = event.getGuild().getSelfMember();
            if (self != null && !self.hasPermission(channel, permissions)) {
                String list = Stream.of(permissions)
                        .filter(e -> !self.hasPermission(channel, e))
                        .map(e -> messageService.getEnumTitle(e))
                        .collect(Collectors.joining("\n"));
                if (self.hasPermission(channel, Permission.MESSAGE_WRITE)) {
                    if (self.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
                        messageService.onError(channel, "discord.command.insufficient.permissions", list);
                    } else {
                        String title = messageService.getMessage("discord.command.insufficient.permissions");
                        String message = messageService.getMessage(list);
                        messageService.sendMessageSilent(channel::sendMessage, title + "\n\n" + message);
                    }
                }
                return true;
            }
        }

        BotContext context = getContext(event.getChannel());
        context.setConfig(guildConfig);

        statisticsService.doWithTimer(getTimer(event.getJDA(), command), () -> {
            try {
                log.debug("Invoke command {} for userId={}, guildId={}",
                        command.getClass().getSimpleName(),
                        event.getAuthor() != null ? event.getAuthor().getId() : null,
                        event.getGuild() != null ? event.getGuild().getId() : null);
                command.doCommand(event, context, content);
                counter.inc();
                if (commandConfig != null && commandConfig.isDeleteSource()
                        && event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                    messageService.delete(event.getMessage(), 1000);
                }
            } catch (ValidationException e) {
                messageService.onEmbedMessage(event.getChannel(), e.getMessage(), e.getArgs());
            } catch (DiscordException e) {
                messageService.onError(event.getChannel(),
                        messageService.hasMessage(e.getMessage()) ? e.getMessage() : "discord.global.error");
                log.error("Command {} execution error", key, e);
            } finally {
                executions.mark();
                registry.incrementCommand(command);
            }
        });
        return true;
    }

    @Override
    public boolean isApplicable(Command command, CommandConfig commandConfig, User user, Member member, TextChannel channel) {
        if (command.getAnnotation() == null) {
            return false;
        }
        if (commandConfig != null && commandConfig.isDisabled()) {
            return false;
        }
        Guild guild = member != null ? member.getGuild() : null;
        if (channel != null) {
            guild = channel.getGuild();
        }
        return command.isAvailable(user, member, guild);
    }

    @Override
    public boolean isRestricted(String rawKey, TextChannel channel, Member member) {
        CommandConfig config = commandConfigService.findByKey(channel.getGuild().getIdLong(), rawKey);
        Command command = holderService.getCommands().get(rawKey);
        if (command == null) {
            return true;
        }

        if (!isApplicable(command, config, member != null ? member.getUser() : null, member, channel)) {
            return true;
        }

        if (isRestricted(config, channel)) {
            return true;
        }

        return member != null && isRestricted(config, member);
    }

    private Timer getTimer(JDA jda, Command command) {
        int shard = -1;
        if (jda != null && jda.getShardInfo() != null) {
            shard = jda.getShardInfo().getShardId();
        }
        return statisticsService.getTimer(String.format("commands/shard.%d/%s", shard, command.getKey()));
    }

    private BotContext getContext(MessageChannel channel) {
        try {
            return contexts.get(channel.getIdLong(), BotContext::new);
        } catch (ExecutionException e) {
            return new BotContext();
        }
    }
}
