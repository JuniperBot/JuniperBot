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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import ru.caramel.juniperbot.web.dto.WelcomeMessageDto;

@Component
public class WelcomeValidator implements Validator {

    @Autowired
    private SpringValidatorAdapter validatorAdapter;

    @Override
    public boolean supports(Class<?> clazz) {
        return WelcomeMessageDto.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validatorAdapter.validate(target, errors);
        WelcomeMessageDto message = (WelcomeMessageDto) target;
        if (message.isJoinEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "joinMessage", "validation.message.content.NotBlank.message");
        }
        if (message.isLeaveEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "leaveMessage", "validation.message.content.NotBlank.message");
        }
    }
}
