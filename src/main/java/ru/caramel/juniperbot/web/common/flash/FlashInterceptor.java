/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
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