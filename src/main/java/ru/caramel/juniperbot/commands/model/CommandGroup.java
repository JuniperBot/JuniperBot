package ru.caramel.juniperbot.commands.model;

import java.util.stream.Stream;

import lombok.Getter;

public enum CommandGroup {

    COMMON("Основное"),
    MUSIC("Музыка"),
    CUSTOM("Пользовательские команды");

    @Getter
    private String title;

    CommandGroup(String title) {
        this.title = title;
    }

    public static CommandGroup getForTitle(String title) {
        return Stream.of(values()).filter(e -> title.equalsIgnoreCase(e.getTitle())).findFirst().orElse(null);
    }
}
