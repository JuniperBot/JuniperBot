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

import static ru.caramel.juniperbot.core.utils.CommonUtils.parseMillis;
import static ru.caramel.juniperbot.core.utils.CommonUtils.parseVkLinks;

public class CommonUtilsTests {

    @Test
    public void testParseMillis() {
        Assert.assertEquals(2000, (long)parseMillis("02"));
        Assert.assertEquals(2000, (long)parseMillis("2"));
        Assert.assertEquals(62000, (long)parseMillis("1:02"));
        Assert.assertEquals(62000, (long)parseMillis("01:02"));
        Assert.assertEquals(3662000 , (long)parseMillis("01:01:02"));
        Assert.assertEquals(3662000 , (long)parseMillis("1:01:02"));
        Assert.assertNull(parseMillis("sdfgdfg"));
        Assert.assertNull(parseMillis(""));
        Assert.assertNull(parseMillis(null));
        Assert.assertNull(parseMillis("60"));
        Assert.assertNull(parseMillis("1:60"));
        Assert.assertNull(parseMillis("25:01:02"));
        Assert.assertNull(parseMillis("12:60:02"));
    }

    @Test
    public void testVkParse() {
        Assert.assertEquals("lol [test2](https://vk.com/test) [test4](https://vk.com/test3)",
                parseVkLinks("lol [test|test2] [test3|test4]"));
    }
}
