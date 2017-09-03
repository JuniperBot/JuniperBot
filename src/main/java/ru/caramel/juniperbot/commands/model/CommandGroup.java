package ru.caramel.juniperbot.commands.model;

public enum CommandGroup {
    COMMON,
    MUSIC,
    CUSTOM;

    @Override
    public String toString() {
        return getClass().getName() + "." + name();
    }
}
