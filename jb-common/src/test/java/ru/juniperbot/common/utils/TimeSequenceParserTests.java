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
package ru.juniperbot.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class TimeSequenceParserTests {

    @Test
    public void test() {
        Assert.assertEquals((Long) 1012L, TimeSequenceParser.parseFull("1 секунду 12 миллисекунд"));
        Assert.assertEquals((Long) 120012L, TimeSequenceParser.parseFull("2 минуты 12 миллисекунд"));
        Assert.assertNull(TimeSequenceParser.parseFull("2 минуты 8299 миллисекунд"));
        Assert.assertNull(TimeSequenceParser.parseFull("9 миллисекунд 2 минуты"));
        Assert.assertEquals((Long) 82L, TimeSequenceParser.parseFull("82 миллисекунд"));

        Assert.assertEquals(31536000000L, TimeSequenceParser.parseShort("1y")); // expects normal (non-leap) year
        Assert.assertEquals(2678400000L, TimeSequenceParser.parseShort("1mos")); // expects 31 day
        Assert.assertEquals(604800000L, TimeSequenceParser.parseShort("1w"));
        Assert.assertEquals(86400000L, TimeSequenceParser.parseShort("1d"));
        Assert.assertEquals(3600000L, TimeSequenceParser.parseShort("1h"));
        Assert.assertEquals(60000L, TimeSequenceParser.parseShort("1min"));
        Assert.assertEquals(1000L, TimeSequenceParser.parseShort("1s"));
        Assert.assertEquals(61000L, TimeSequenceParser.parseShort("1min1s"));
        Assert.assertEquals(61000L, TimeSequenceParser.parseShort("1MIN1S"));
        Assert.assertEquals(60000L, TimeSequenceParser.parseShort("60s"));

        Assert.assertEquals(31536000000L, TimeSequenceParser.parseShort("1г")); // expects normal (non-leap) year
        Assert.assertEquals(2678400000L, TimeSequenceParser.parseShort("1мес")); // expects 31 day
        Assert.assertEquals(604800000L, TimeSequenceParser.parseShort("1н"));
        Assert.assertEquals(86400000L, TimeSequenceParser.parseShort("1д"));
        Assert.assertEquals(3600000L, TimeSequenceParser.parseShort("1ч"));
        Assert.assertEquals(60000L, TimeSequenceParser.parseShort("1мин"));
        Assert.assertEquals(1000L, TimeSequenceParser.parseShort("1с"));
        Assert.assertEquals(61000L, TimeSequenceParser.parseShort("1мин1с"));
        Assert.assertEquals(61000L, TimeSequenceParser.parseShort("1МИН1С"));
        Assert.assertEquals(60000L, TimeSequenceParser.parseShort("60с"));
    }
}
