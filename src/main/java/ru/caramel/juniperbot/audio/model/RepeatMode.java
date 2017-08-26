package ru.caramel.juniperbot.audio.model;

import lombok.Getter;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RepeatMode {
    CURRENT("этой", ":repeat_one:"),
    ALL("всех", ":repeat:"),
    NONE("выкл", ":arrow_forward:");

    @Getter
    private final String title;

    @Getter
    private final String emoji;

    RepeatMode(String title, String emoji) {
        this.title = title;
        this.emoji = emoji;
    }

    public String getFullTitle() {
        return title + " " + emoji;
    }

    public static RepeatMode getForTitle(String title) {
        return Stream.of(values()).filter(e -> Objects.equals(e.title, title)).findFirst().orElse(null);
    }

    public static String options() {
        return Stream.of(values()).map(RepeatMode::getTitle).collect(Collectors.joining("|"));
    }
}
