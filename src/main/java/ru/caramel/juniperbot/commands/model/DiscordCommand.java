package ru.caramel.juniperbot.commands.model;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Inherited
public @interface DiscordCommand {

    String key();

    String description();

    CommandSource[] source() default {};

    CommandGroup group() default CommandGroup.COMMON;

    int priority() default 1;

    boolean hidden() default false;
}
