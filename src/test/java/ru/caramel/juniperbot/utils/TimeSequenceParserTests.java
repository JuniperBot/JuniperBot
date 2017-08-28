package ru.caramel.juniperbot.utils;

import org.junit.Assert;
import org.junit.Test;

public class TimeSequenceParserTests {

    private TimeSequenceParser parser = new TimeSequenceParser();

    @Test
    public void test() {
        Assert.assertEquals((Long)1012L, parser.parse("1 секунду 12 миллисекунд"));
        Assert.assertEquals((Long)120012L, parser.parse("2 минуты 12 миллисекунд"));
        Assert.assertNull(parser.parse("2 минуты 8299 миллисекунд"));
        Assert.assertNull(parser.parse("9 миллисекунд 2 минуты"));
        Assert.assertEquals((Long)82L, parser.parse("82 миллисекунд"));
    }
}
