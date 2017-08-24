package ru.caramel.juniperbot.integration.discord.model;

public class DiscordException extends Exception {

    private static final long serialVersionUID = 4621225391411561750L;

    public DiscordException() {
    }

    public DiscordException(String message) {
        super(message);
    }

    public DiscordException(String message, Throwable cause) {
        super(message, cause);
    }
}
