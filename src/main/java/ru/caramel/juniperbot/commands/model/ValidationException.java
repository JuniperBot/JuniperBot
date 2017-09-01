package ru.caramel.juniperbot.commands.model;

import ru.caramel.juniperbot.integration.discord.model.DiscordException;

public class ValidationException extends DiscordException {

    private static final long serialVersionUID = 2474643174485112049L;

    public ValidationException(String message, Object... args) {
        super(message, args);
    }

    public ValidationException(String message, Throwable cause, Object... args) {
        super(message, cause, args);
    }
}
