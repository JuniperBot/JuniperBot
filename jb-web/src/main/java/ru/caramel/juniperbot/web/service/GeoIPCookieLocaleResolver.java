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
package ru.caramel.juniperbot.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import ru.caramel.juniperbot.core.service.ContextService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

@Service
public class GeoIPCookieLocaleResolver extends CookieLocaleResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIPCookieLocaleResolver.class);

    @Autowired
    private GeoIPService geoIPService;

    @Autowired
    private ContextService contextService;

    /**
     * Determine the default locale for the given request,
     * Called if no locale cookie has been found.
     * <p>The default implementation returns the specified default locale,
     * if any, else falls back to the request's accept-header locale.
     * @param request the request to resolve the locale for
     * @return the default locale (never {@code null})
     * @see #setDefaultLocale
     * @see javax.servlet.http.HttpServletRequest#getLocale()
     */
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        try {
            String name = geoIPService.getLocale(request);
            if (name != null) {
                Locale locale = contextService.getSupportedLocales().get(name);
                if (locale != null) {
                    return locale;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Locale detect error", e);
        }
        return super.determineDefaultLocale(request);
    }
}
