package ru.caramel.juniperbot.commands;

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

public interface Command {

    boolean doCommand(MessageReceivedEvent message, BotContext context, String content) throws DiscordException;

    default boolean isApplicable(MessageChannel channel, GuildConfig config) {
        if (!getClass().isAnnotationPresent(DiscordCommand.class)) {
            return false;
        }
        DiscordCommand command = getClass().getAnnotation(DiscordCommand.class);
        if (config != null && ArrayUtils.contains(config.getDisabledCommands(), command.key())) {
            return false;
        }
        if (command.source().length == 0) {
            return true;
        }
        CommandSource source = channel instanceof TextChannel ? CommandSource.GUILD : CommandSource.DM;
        return ArrayUtils.contains(command.source(), source);
    }
}
