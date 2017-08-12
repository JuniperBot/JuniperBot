package ru.caramel.juniperbot.configuration;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.AccountType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.util.List;
import java.util.Objects;

@Component
public class DiscordConfig {

    public static final int MAX_DETAILED = 3;

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
    private Color accentColor;

    @Getter
    @Setter
    private Long playRefreshInterval;

    @Getter
    @Setter
    private String playingStatus;

    @Getter
    @Setter
    private List<DiscordWebHook> webHooks;

    @Getter
    @Setter
    private String copyImageUrl;

    @Getter
    @Setter
    private String copyContent;

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

    public void setAccentColor(String color) {
        accentColor = StringUtils.isNotEmpty(color) ? Color.decode(color) : null;
    }

    public static class DiscordWebHook {

        @Getter
        @Setter
        private long id;

        @Getter
        @Setter
        private String token;

    }
}
