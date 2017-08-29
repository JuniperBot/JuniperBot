package ru.caramel.juniperbot.web.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.model.CommandType;
import ru.caramel.juniperbot.model.CommandsContainer;
import ru.caramel.juniperbot.service.CommandsService;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;
import ru.caramel.juniperbot.web.common.validation.CommandsContainerValidator;

@Controller
@Navigation(PageElement.CONFIG_COMMANDS)
public class CommandsController extends AbstractController {

    @Autowired
    private CommandsService commandsService;

    @Autowired
    private CommandsContainerValidator validator;

    @InitBinder
    public void init(WebDataBinder binder) {
        binder.setValidator(validator);
    }

    @RequestMapping("/commands/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        return createModel("commands", serverId)
                .addObject("commandsContainer",
                        new CommandsContainer(commandsService.getCustomForView(serverId)));
    }

    @RequestMapping(value = "/commands/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @Validated @ModelAttribute("commandsContainer") CommandsContainer container,
            BindingResult result) {
        validateGuildId(serverId);
        if (result.hasErrors()) {
            return createModel("commands", serverId);
        }
        commandsService.saveCommands(container.getCommands(), serverId);
        flash.success("flash.commands.save.success.message");
        return view(serverId);
    }

    protected ModelAndView createModel(String model, long serverId) {
        return super.createModel(model, serverId)
                .addObject("commandTypes", CommandType.values())
                .addObject("commandPrefix", configService.getConfig(serverId).getPrefix());
    }
}
