/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
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

    private String copyImageUrl;

    private String copyContent;

    private String superUserId;

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
}
