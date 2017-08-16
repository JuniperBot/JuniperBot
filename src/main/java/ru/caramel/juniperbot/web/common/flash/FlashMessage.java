package ru.caramel.juniperbot.web.common.flash;

import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;

import java.io.Serializable;

public class FlashMessage implements Serializable {

    private static final long serialVersionUID = -6146729966504725892L;

    @Getter
    private final String key;

    @Getter
    private final Flash.FlashType type;

    @Getter
    private final MessageSourceResolvable resolvable;

    public FlashMessage(Flash.FlashType type, String key, MessageSourceResolvable resolvable) {
        this.type = type;
        this.key = key;
        this.resolvable = resolvable;
    }
}
