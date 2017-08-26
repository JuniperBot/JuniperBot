package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
public class VkConnectionDto implements Serializable {

    private static final long serialVersionUID = -3440888869725084354L;

    @NotNull
    private Long id;

    private String token;

    private String name;

    private VkConnectionStatus status;

    @Valid
    private WebHookDto webHook;

}
