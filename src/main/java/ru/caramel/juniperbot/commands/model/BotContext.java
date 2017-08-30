package ru.caramel.juniperbot.commands.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RequestFuture;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;

import java.util.List;

@Getter
@Setter
public class BotContext {

    private Guild guild;

    private String prefix;

    private GuildConfig config;

    private Message searchMessage;

    private List<String> searchResults;

    private List<RequestFuture<Void>> searchActions;
}
