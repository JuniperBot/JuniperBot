package ru.caramel.juniperbot.web.common.flash;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import java.util.List;

public class FlashInterceptor implements WebRequestInterceptor {

    @Autowired
    private Flash flash;

    @Override
    public void preHandle(WebRequest request) {
        final List<FlashMessage> messages = flash.getMessages();
        request.setAttribute("flash", messages, RequestAttributes.SCOPE_REQUEST);
        for (FlashMessage message : messages) {
            final String key = "flash" + message.getResolvable().getCodes()[0];
            request.setAttribute(key, message, RequestAttributes.SCOPE_REQUEST);
        }
        flash.reset();
    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {
        // nothing to do
    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {
        // nothing to do
    }
}