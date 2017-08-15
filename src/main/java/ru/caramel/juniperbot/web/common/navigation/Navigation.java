package ru.caramel.juniperbot.web.common.navigation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Navigation {

    PageElement value();

}
