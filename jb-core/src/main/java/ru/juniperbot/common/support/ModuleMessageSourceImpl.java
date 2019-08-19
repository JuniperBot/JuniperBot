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
package ru.juniperbot.common.support;

import org.springframework.context.support.ResourceBundleMessageSource;

import java.text.MessageFormat;
import java.util.Locale;

public class ModuleMessageSourceImpl extends ResourceBundleMessageSource implements ModuleMessageSource {

    public ModuleMessageSourceImpl() {
        setDefaultEncoding("UTF-8");
    }

    /**
     * Resolves the given message code as key in the registered resource bundles,
     * returning the value found in the bundle as-is (without MessageFormat parsing).
     */
    @Override
    public String resolveCodeWithoutArguments(String code, Locale locale) {
        return super.resolveCodeWithoutArguments(code, locale);
    }

    /**
     * Resolves the given message code as key in the registered resource bundles,
     * returning the value found in the bundle as-is (without MessageFormat parsing).
     */
    @Override
    public MessageFormat resolveCode(String code, Locale locale) {
        return super.resolveCode(code, locale);
    }
}
