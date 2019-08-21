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

    private TimeSequenceParser parser = new TimeSequenceParser();

    @Test
    public void test() {
        Assert.assertEquals((Long) 1012L, parser.parse("1 секунду 12 миллисекунд"));
        Assert.assertEquals((Long) 120012L, parser.parse("2 минуты 12 миллисекунд"));
        Assert.assertNull(parser.parse("2 минуты 8299 миллисекунд"));
        Assert.assertNull(parser.parse("9 миллисекунд 2 минуты"));
        Assert.assertEquals((Long) 82L, parser.parse("82 миллисекунд"));
    }
}
