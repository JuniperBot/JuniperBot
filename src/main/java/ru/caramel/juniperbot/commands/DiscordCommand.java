package ru.caramel.juniperbot.commands;

import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.model.CommandSource;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Inherited
public @interface DiscordCommand {

    String key();

    String description();

    CommandSource[] source() default {};

    boolean hidden() default false;
}
