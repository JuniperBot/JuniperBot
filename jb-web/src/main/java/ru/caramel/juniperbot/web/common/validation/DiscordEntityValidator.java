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
package ru.caramel.juniperbot.web.common.validation;

import org.apache.commons.lang3.StringUtils;
import ru.caramel.juniperbot.web.security.model.DiscordUserDetails;
import ru.caramel.juniperbot.web.security.utils.SecurityUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Check if validated entity is owned by current context guild
 */
public class DiscordEntityValidator implements ConstraintValidator<DiscordEntity, String> {

    private final static Pattern PATTERN = Pattern.compile("^(\\d{1,19})?$");

    private DiscordEntityType type;

    private boolean allowDm;

    private boolean strict;

    @Override
    public void initialize(DiscordEntity constraintAnnotation) {
        type = constraintAnnotation.value();
        allowDm = constraintAnnotation.allowDm();
        strict = constraintAnnotation.strict();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        if (DiscordEntityType.TEXT_CHANNEL == type && "-1".equals(value)) {
            return allowDm;
        }
        if (!PATTERN.matcher(value).matches()) {
            return false;
        }

        DiscordUserDetails userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) {
            return false;
        }

        if (!strict) {
            return true;
        }

        /*
        // TODO IMPLEMENT MEMBER CHECK

        Guild guild = null;
        switch (type) {
            case TEXT_CHANNEL:
                TextChannel textChannel = discordService.getTextChannelById(value);
                if (textChannel != null) {
                    guild = textChannel.getGuild();
                }
                break;
            case VOICE_CHANNEL:
                VoiceChannel voiceChannel = discordService.getVoiceChannelById(value);
                if (voiceChannel != null) {
                    guild = voiceChannel.getGuild();
                }
                break;
        }

        if (guild == null) {
            return true;
        }

        Member member = guild.getMemberById(userDetails.getId());
        if (member == null) {
            return false;
        }

        return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR);
        */
        return true;
    }
}
