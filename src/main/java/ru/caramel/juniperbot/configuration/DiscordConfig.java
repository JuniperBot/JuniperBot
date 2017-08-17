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

@Getter
@Setter
@Component
public class DiscordConfig {

    public static final int MAX_DETAILED = 3;

    private String prefix;

    private String token;

    private AccountType accountType;

    private String userName;

    private String avatarUrl;

    private Color accentColor;

    private Long playRefreshInterval;

    private String playingStatus;

    private List<DiscordWebHook> webHooks;

    private String copyImageUrl;

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

    @Getter
    @Setter
    public static class DiscordWebHook {

        private long id;

        private String token;
    }
}
