package ru.caramel.juniperbot.model.enums;

public enum CommandType {
    MESSAGE, ALIAS;

    @Override
    public String toString() {
        return getClass().getName() + "." + name();
    }
}
