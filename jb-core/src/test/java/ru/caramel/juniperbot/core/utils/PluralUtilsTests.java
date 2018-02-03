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
package ru.caramel.juniperbot.core.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

import static ru.caramel.juniperbot.core.utils.PluralUtils.*;

public class PluralUtilsTests {

    @Test
    public void testRuPlurals() {
        Locale ruLocale = Locale.forLanguageTag("ru-RU");
        Assert.assertEquals("zero", getPluralKey(ruLocale, 0));
        Assert.assertEquals("zero", getPluralKey(ruLocale, 10));
        Assert.assertEquals("zero", getPluralKey(ruLocale, 30));

        Assert.assertEquals("one", getPluralKey(ruLocale, 1));
        Assert.assertEquals("many", getPluralKey(ruLocale, 11));
        Assert.assertEquals("one", getPluralKey(ruLocale, 21));

        Assert.assertEquals("two", getPluralKey(ruLocale, 2));
        Assert.assertEquals("many", getPluralKey(ruLocale, 12));
        Assert.assertEquals("two", getPluralKey(ruLocale, 22));

        Assert.assertEquals("few", getPluralKey(ruLocale, 3));
        Assert.assertEquals("many", getPluralKey(ruLocale, 13));
        Assert.assertEquals("few", getPluralKey(ruLocale, 23));

        Assert.assertEquals("many", getPluralKey(ruLocale, 55));
    }

    @Test
    public void testEnPlurals() {
        Locale ruLocale = Locale.ENGLISH;
        Assert.assertEquals("zero", getPluralKey(ruLocale, 0));

        Assert.assertEquals("one", getPluralKey(ruLocale, 1));
        Assert.assertEquals("other", getPluralKey(ruLocale, 11));
        Assert.assertEquals("other", getPluralKey(ruLocale, 21));

        Assert.assertEquals("other", getPluralKey(ruLocale, 2));
        Assert.assertEquals("other", getPluralKey(ruLocale, 12));
        Assert.assertEquals("other", getPluralKey(ruLocale, 22));

        Assert.assertEquals("other", getPluralKey(ruLocale, 3));
        Assert.assertEquals("other", getPluralKey(ruLocale, 13));
        Assert.assertEquals("other", getPluralKey(ruLocale, 23));

        Assert.assertEquals("other", getPluralKey(ruLocale, 55));
    }
}
