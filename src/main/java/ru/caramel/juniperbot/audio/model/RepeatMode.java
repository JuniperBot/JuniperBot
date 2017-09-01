package ru.caramel.juniperbot.audio.model;

import lombok.Getter;

public enum RepeatMode {
    CURRENT(":repeat_one:"),
    ALL(":repeat:"),
    NONE(":arrow_forward:");

    @Getter
    private final String emoji;

    RepeatMode(String emoji) {
        this.emoji = emoji;
    }
}
