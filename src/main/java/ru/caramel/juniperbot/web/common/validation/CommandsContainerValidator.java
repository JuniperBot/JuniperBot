package ru.caramel.juniperbot.web.common.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import ru.caramel.juniperbot.model.CommandsContainer;
import ru.caramel.juniperbot.model.CustomCommandDto;
import ru.caramel.juniperbot.service.CommandsService;

import java.util.HashSet;
import java.util.Set;

@Component
public class CommandsContainerValidator implements Validator {

    @Autowired
    private SpringValidatorAdapter validatorAdapter;

    @Autowired
    private CommandsService commandsService;

    @Override
    public boolean supports(Class<?> clazz) {
        return CommandsContainer.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validatorAdapter.validate(target, errors);
        Set<String> internalCommands = commandsService.getCommands().keySet();
        CommandsContainer container = (CommandsContainer) target;

        Set<String> existingKey = new HashSet<>();
        for (int i = 0; i < container.getCommands().size(); i++) {
            String path = "commands[" + i + "].";
            CustomCommandDto command = container.getCommands().get(i);
            String key = command.getKey();
            if (StringUtils.isNotEmpty(key)) {
                if (!existingKey.add(key)) {
                    errors.rejectValue(path + "key", "validation.commands.key.unique.message");
                } else if (internalCommands.contains(key)) {
                    errors.rejectValue(path + "key", "validation.commands.key.service.message");
                }
            }
        }
    }
}
