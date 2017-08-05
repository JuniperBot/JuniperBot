package ru.caramel.juniperbot.model.exception;

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
