package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.commands.model.DiscordCommand;

import java.io.Serializable;

@Getter
@Setter
public class CommandTypeDto implements Serializable {
    private static final long serialVersionUID = 1328230463233769540L;
    private final String key;
    private final String description;

    public CommandTypeDto(DiscordCommand command) {
        key = command.key();
        description = command.description();
    }
}
