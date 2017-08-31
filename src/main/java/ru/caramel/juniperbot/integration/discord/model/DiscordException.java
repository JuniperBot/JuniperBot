package ru.caramel.juniperbot.integration.discord.model;

import lombok.Getter;

public class DiscordException extends Exception {

    private static final long serialVersionUID = 4621225391411561750L;

    @Getter
    private final Object[] args;

    public DiscordException() {
        args = null;
    }

    public DiscordException(String message, Object... args) {
        this(message, null, args);
    }

    public DiscordException(String message, Throwable cause, Object... args) {
        super(message, cause);
        this.args = args;
    }
}
