package ru.caramel.juniperbot.commands.model;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

@Getter
@Setter
public class BotContext {

    private String prefix;

    private GuildConfig config;
}
