package ru.caramel.juniperbot.module.custom.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.core.command.model.BotContext;
import ru.caramel.juniperbot.core.command.model.CommandExtension;
import ru.caramel.juniperbot.core.command.persistence.CommandConfig;
import ru.caramel.juniperbot.core.command.service.CommandsService;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.module.custom.persistence.entity.CustomCommand;
import ru.caramel.juniperbot.module.custom.persistence.repository.CustomCommandRepository;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomCommandsExtension implements CommandExtension {

    public static final String GROUP_KEY = "discord.command.group.custom";

    @Autowired
    private CustomCommandRepository commandRepository;

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private MessageService messageService;

    public void extendHelp(MessageReceivedEvent event, BotContext context, EmbedBuilder embedBuilder) {
        // Пользовательские команды
        if (event.getChannelType().isGuild() && context.getConfig() != null) {
            List<CustomCommand> commands = commandRepository.findAllByGuildId(context.getConfig().getGuildId()).stream()
                    .filter(e -> {
                        CommandConfig config = e.getCommandConfig();
                        return config == null || !config.isDisabled()
                                && !commandsService.isRestricted(config, event.getTextChannel())
                                && !commandsService.isRestricted(config, event.getMember());
                    })
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(commands)) {
                StringBuilder list = new StringBuilder();
                commands.forEach(e -> {
                    if (list.length() > 0) {
                        list.append(", ");
                    }
                    list.append('`').append(context.getConfig().getPrefix()).append(e.getKey()).append('`');
                });
                if (list.length() > 0) {
                    embedBuilder.addField(messageService.getMessage(GROUP_KEY) + ":",
                            list.toString(), false);
                }
            }
        }
    }
}
