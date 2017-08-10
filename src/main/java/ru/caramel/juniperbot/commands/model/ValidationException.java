package ru.caramel.juniperbot.commands.model;

import ru.caramel.juniperbot.integration.discord.model.DiscordException;

public class ValidationException extends DiscordException {

    public ValidationException() {
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
