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
package ru.caramel.juniperbot.web.common.tags;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import ru.caramel.juniperbot.core.service.LocaleService;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.Locale;

public class CommandTag extends RequestContextAwareTag {

    private static final long serialVersionUID = -5188535954386072251L;

    private static final String LOCALE_ATTR = "CommandTag.Locale";

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String var;

    @Override
    protected int doStartTagInternal() throws Exception {
        try {
            String result = code;
            Long serverId = (Long) pageContext.getRequest().getAttribute("serverId");
            if (serverId != null) {
                Locale locale = (Locale) pageContext.getAttribute(LOCALE_ATTR);
                ApplicationContext context = getRequestContext().getWebApplicationContext();
                if (locale == null) {
                    LocaleService localeService = context.getBean(LocaleService.class);
                    locale = localeService.getLocale(serverId);
                    pageContext.setAttribute(LOCALE_ATTR, locale);
                }
                result = context.getMessage(code, null, locale);
            }
            if (StringUtils.isNotEmpty(var)) {
                pageContext.setAttribute(var, result);
            } else {
                JspWriter out = pageContext.getOut();
                out.write(result);
            }
        } catch (Exception ex) {
            throw new JspException(ex);
        }
        return SKIP_BODY;
    }
}
