package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.MessageChannel;

public class BotContext {

    @Getter
    @Setter
    private boolean detailedEmbed = true;

    @Getter
    @Setter
    private String latestId;

    @Getter
    private final MessageChannel channel;

    public BotContext(MessageChannel channel) {
        this.channel = channel;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof BotContext)) {
            return false;
        }
        BotContext otherContext = (BotContext) other;
        return otherContext.channel.equals(this.channel);
    }
}
