/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api.common;

import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.TreeMap;

public class ApiRequestLoggingFilter extends AbstractRequestLoggingFilter {

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        logger.info(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        logger.info(message);
    }

    @Override
    protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
        StringBuilder msg = new StringBuilder()
                .append(prefix)
                .append(request.getMethod())
                .append(" [")
                .append(request.getRequestURI());

        if (isIncludeQueryString()) {
            String queryString = request.getQueryString();
            if (queryString != null) {
                msg.append('?').append(queryString);
            }
        }

        msg.append("]");

        Map<String, String> data = new TreeMap<>();

        if (isIncludeClientInfo()) {
            String client = request.getHeader("X-Real-IP");
            if (!StringUtils.hasLength(client)) {
                client = request.getRemoteAddr();
            }
            if (StringUtils.hasLength(client)) {
                data.put("client", client);
            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                data.put("session", session.getId());
            }
            String user = request.getRemoteUser();
            if (user != null) {
                data.put("user", user);
            }
        }

        if (isIncludeHeaders()) {
            data.put("headers", new ServletServerHttpRequest(request).getHeaders().toString());
        }

        if (isIncludePayload()) {
            String payload = getMessagePayload(request);
            if (payload != null) {
                data.put("payload", payload);
            }
        }

        if (!data.isEmpty()) {
            msg.append(" ").append(data.toString());
        }

        msg.append(suffix);
        return msg.toString();
    }
}
