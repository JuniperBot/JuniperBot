package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CommandsContainer {

    @Valid
    private List<CustomCommandDto> commands;

    public CommandsContainer(List<CustomCommandDto> commands) {
        this.commands = commands;
    }
}