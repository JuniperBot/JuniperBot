package ru.caramel.juniperbot.web.common.flash;

import java.io.Serializable;
import java.util.List;

public interface Flash {

    enum FlashType {
        INFO, ERROR, WARNING, SUCCESS;
    }

    void info(String key, Serializable... arguments);

    void error(String key, Serializable... arguments);

    void warn(String key, Serializable... arguments);

    void success(String key, Serializable... arguments);

    List<FlashMessage> getMessages();

    void reset();
}