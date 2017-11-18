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
package ru.caramel.juniperbot.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Objects;

@Controller
public class LetsEncryptController {

    private static final String PARAM_CONTENT = "letsencrypt.content";

    @Value("${letsencrypt.secret:}")
    private String secret;

    @RequestMapping("/.well-known/acme-challenge/set/{content}")
    public @ResponseBody String set(HttpSession session, @PathVariable("content") String content, @RequestParam("secret") String secret) {
        if (StringUtils.isEmpty(this.secret) || !Objects.equals(this.secret, secret)) {
            return "FAIL!";
        }
        session.setAttribute(PARAM_CONTENT, content);
        return "OK!";
    }

    @RequestMapping(value = "/.well-known/acme-challenge/{token}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String message(HttpSession session, HttpServletResponse response, @PathVariable("token") String token) {//REST Endpoint.
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        return (String) session.getAttribute(PARAM_CONTENT);
    }
}
