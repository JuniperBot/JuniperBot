package ru.caramel.juniperbot.commands.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.ArrayList;
import java.util.List;

public class BotContext {

    @Getter
    @Setter
    private String latestId;

    @Getter
    @Setter
    private List<MessageChannel> subscriptions = new ArrayList<>();

}
