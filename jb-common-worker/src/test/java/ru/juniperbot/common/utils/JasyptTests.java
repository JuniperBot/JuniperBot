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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JasyptTests {

    private static final int DIFFICULTY = 200;

    private static final String PASSWORD = "testmypassword";

    private static final AES256TextEncryptor encryptor;

    static {
        encryptor = new AES256TextEncryptor();
        encryptor.setPassword(PASSWORD);
    }

    @BeforeClass
    public static void init() {
        log.info("Initialization encryptors (hot start)");
        encryptor.encrypt(getRandomString());
    }

    @Test
    public void singleTextOneEncryptor() {
        encryptor.encrypt(getRandomString());
    }

    @Test
    public void multipleTextsOneInstance() {
        List<String> strings = getRandomStrings(DIFFICULTY);
        strings.forEach(encryptor::encrypt);
    }

    @Test
    public void multipleTextsOneInstanceParallel() {
        List<String> strings = getRandomStrings(DIFFICULTY);
        strings.parallelStream().forEach(encryptor::encrypt);
    }

    @Test
    public void multipleTextsOneInstancePooled() {
        List<String> strings = getRandomStrings(DIFFICULTY);
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPoolSize(4);
        encryptor.setPassword(PASSWORD);
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        strings.parallelStream().forEach(encryptor::encrypt);
    }

    @Test
    public void multipleTextsManyInstances() {
        List<String> strings = getRandomStrings(DIFFICULTY);
        strings.forEach(e -> {
            AES256TextEncryptor encryptor = new AES256TextEncryptor();
            encryptor.setPassword(PASSWORD);
            encryptor.encrypt(e);
        });
    }

    @Test
    public void multipleTextsManyInstancesParallel() {
        List<String> strings = getRandomStrings(DIFFICULTY);
        strings.parallelStream().forEach(e -> {
            AES256TextEncryptor encryptor = new AES256TextEncryptor();
            encryptor.setPassword(PASSWORD);
            encryptor.encrypt(e);
        });
    }

    private static String getRandomString() {
        return RandomStringUtils.randomAlphabetic(20);
    }

    private static List<String> getRandomStrings(int count) {
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(getRandomString());
        }
        return result;
    }
}
