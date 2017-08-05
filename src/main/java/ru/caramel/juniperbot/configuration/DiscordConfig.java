package ru.caramel.juniperbot.configuration;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.AccountType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

@Component
public class DiscordConfig {

    @Setter
    private String prefix;

    @Getter
    @Setter
    private String token;

    @Setter
    private AccountType accountType;

    @Getter
    @Setter
    private String userName;

    @Getter
    @Setter
    private String avatarUrl;

    @Getter
    @Setter
    private List<DiscordWebHook> webHooks;

    @PostConstruct
    public void validate() {
        Objects.requireNonNull(token, "No discord token specified!");
    }

    public String getPrefix() {
        return prefix != null ? prefix : "!";
    }

    public AccountType getAccountType() {
        return accountType != null ? accountType : AccountType.BOT;
    }

    public static class DiscordWebHook {

        @Getter @Setter
        private long id;

        @Getter @Setter
        private String token;

    }
}
