package ru.caramel.juniperbot.web.configuration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.model.CommandTypeDto;
import ru.caramel.juniperbot.model.CommandsDto;
import ru.caramel.juniperbot.persistence.entity.GuildConfig;
import ru.caramel.juniperbot.service.CommandsService;
import ru.caramel.juniperbot.utils.ArrayUtil;
import ru.caramel.juniperbot.web.common.AbstractController;
import ru.caramel.juniperbot.web.common.navigation.Navigation;
import ru.caramel.juniperbot.web.common.navigation.PageElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Navigation(PageElement.CONFIG_COMMANDS)
public class CommandsController extends AbstractController {

    @Autowired
    private CommandsService commandsService;

    @RequestMapping("/commands/{serverId}")
    public ModelAndView view(@PathVariable long serverId) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId);
        CommandsDto dto = new CommandsDto();
        dto.setCommands(ArrayUtil.reverse(String[].class, config.getDisabledCommands(), commandsService.getCommands().keySet()));
        return createModel("commands", serverId)
                .addObject("commandsContainer", dto);
    }

    @RequestMapping(value = "/commands/{serverId}", method = RequestMethod.POST)
    public ModelAndView save(
            @PathVariable long serverId,
            @ModelAttribute("commandsContainer") CommandsDto container) {
        validateGuildId(serverId);
        GuildConfig config = configService.getOrCreate(serverId);
        config.setDisabledCommands(ArrayUtil.reverse(String[].class, container.getCommands(), commandsService.getCommands().keySet()));
        configService.save(config);
        flash.success("flash.commands.save.success.message");
        return view(serverId);
    }

    protected ModelAndView createModel(String model, long serverId) {
        Map<CommandGroup, List<CommandTypeDto>> descriptors = new LinkedHashMap<>();
        commandsService.getDescriptors().forEach((group, descriptor) -> {
            if (CommandGroup.CUSTOM.equals(group)) return;
            descriptors.put(group, descriptor.stream().map(CommandTypeDto::new).collect(Collectors.toList()));
        });
        return super.createModel(model, serverId)
                .addObject("commandTypes", descriptors)
                .addObject("commandPrefix", configService.getConfig(serverId).getPrefix());
    }
}
