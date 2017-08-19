package ru.caramel.juniperbot.web.common.flash;

import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component("flash")
@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
public class FlashImpl implements Flash {

    @Getter
    private List<FlashMessage> messages = new ArrayList<>();

    @Override
    public void info(String key, Serializable... arguments) {
        addMessage(FlashType.INFO, key, arguments);
    }

    @Override
    public void error(String key, Serializable... arguments) {
        addMessage(FlashType.ERROR, key, arguments);
    }

    @Override
    public void warn(String key, Serializable... arguments) {
        addMessage(FlashType.WARNING, key, arguments);
    }

    @Override
    public void success(String key, Serializable... arguments) {
        addMessage(FlashType.SUCCESS, key, arguments);
    }

    private void addMessage(FlashType type, String key, Serializable... arguments) {
        MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[] { key }, arguments);
        FlashMessage message = new FlashMessage(type, key, resolvable);
        messages.add(message);
    }

    @Override
    public void reset() {
        messages.clear();
    }
}
