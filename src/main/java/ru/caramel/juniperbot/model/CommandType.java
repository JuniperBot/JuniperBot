package ru.caramel.juniperbot.model;

public enum CommandType {
    MESSAGE, ALIAS;

    @Override
    public String toString() {
        return getClass().getName() + "." + name();
    }
}
