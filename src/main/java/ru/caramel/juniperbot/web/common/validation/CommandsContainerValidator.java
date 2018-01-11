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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import ru.caramel.juniperbot.core.model.CommandsContainer;
import ru.caramel.juniperbot.modules.customcommand.model.CustomCommandDto;
import ru.caramel.juniperbot.core.service.CommandsHolderService;

import java.util.HashSet;
import java.util.Set;

@Component
public class CommandsContainerValidator implements Validator {

    @Autowired
    private SpringValidatorAdapter validatorAdapter;

    @Autowired
    private CommandsHolderService holderService;

    @Override
    public boolean supports(Class<?> clazz) {
        return CommandsContainer.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validatorAdapter.validate(target, errors);
        CommandsContainer container = (CommandsContainer) target;

        Set<String> existingKey = new HashSet<>();
        for (int i = 0; i < container.getCommands().size(); i++) {
            String path = "commands[" + i + "].";
            CustomCommandDto command = container.getCommands().get(i);
            String key = command.getKey();
            if (StringUtils.isNotEmpty(key)) {
                if (!existingKey.add(key)) {
                    errors.rejectValue(path + "key", "validation.commands.key.unique.message");
                } else if (holderService.getByLocale(key, true) != null) {
                    errors.rejectValue(path + "key", "validation.commands.key.service.message");
                }
            }
        }
    }
}
