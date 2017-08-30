package ru.caramel.juniperbot.model;

import lombok.Getter;

public enum  CommandType {
    MESSAGE("Сообщение"),
    ALIAS("Перенаправление");

    @Getter
    private String title;

    CommandType(String title) {
        this.title = title;
    }
}
