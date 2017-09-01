package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import ru.caramel.juniperbot.model.enums.CommandType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Getter
@Setter
public class CustomCommandDto implements Serializable {
    private static final long serialVersionUID = 6050850842749762636L;

    private Long id;

    @NotNull
    private CommandType type;

    @Size(max = 25, message = "{validation.commands.key.Size.message}")
    @NotBlank(message = "{validation.commands.key.NotBlank.message}")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я0-9]*$", message = "{validation.commands.key.pattern.message}")
    private String key;

    @NotBlank(message = "{validation.commands.content.NotBlank.message}")
    private String content;
}
