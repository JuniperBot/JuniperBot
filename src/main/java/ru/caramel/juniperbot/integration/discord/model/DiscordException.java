package ru.caramel.juniperbot.integration.discord.model;

public class DiscordException extends Exception {

    public DiscordException() {
    }

    public DiscordException(String message) {
        super(message);
    }

    public DiscordException(String message, Throwable cause) {
        super(message, cause);
    }
}
