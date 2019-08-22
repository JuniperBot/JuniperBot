/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api.common.validation;

import net.dv8tion.jda.api.entities.ChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.juniperbot.api.security.model.DiscordUserDetails;
import ru.juniperbot.api.security.utils.SecurityUtils;
import ru.juniperbot.common.model.request.CheckOwnerRequest;
import ru.juniperbot.common.service.GatewayService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Check if validated entity is owned by current context guild
 */
public class DiscordEntityValidator implements ConstraintValidator<DiscordEntity, String> {

    private final static Pattern PATTERN = Pattern.compile("^(\\d{1,19})?$");

    private ChannelType type;

    private boolean allowDm;

    private boolean strict;

    @Autowired
    private GatewayService gatewayService;

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
        if (ChannelType.TEXT == type && "-1".equals(value)) {
            return allowDm;
        }
        if (!PATTERN.matcher(value).matches()) {
            return false;
        }

        DiscordUserDetails userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) {
            return false;
        }

        return !strict || gatewayService.isChannelOwner(CheckOwnerRequest.builder()
                .type(type)
                .channelId(value)
                .userId(userDetails.getId())
                .build());
    }
}
