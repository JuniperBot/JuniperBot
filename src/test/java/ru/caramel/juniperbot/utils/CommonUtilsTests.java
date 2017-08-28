package ru.caramel.juniperbot.utils;

import org.junit.Assert;
import org.junit.Test;

import static ru.caramel.juniperbot.utils.CommonUtils.*;

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
}
