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
import net.sf.jasperreports.engine.*;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.juniperbot.common.worker.utils.JasperReportsUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JasperReportsTests {

    private static JasperReport report;

    @BeforeClass
    public static void init() throws JRException {
        InputStream employeeReportStream = JasperReportsTests.class.getResourceAsStream("/visiting_card.jrxml");
        report = JasperCompileManager.compileReport(employeeReportStream);
    }

    @Test
    public void generate() throws IOException, JRException {
        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("name", "Harshvardhan Singh");
        templateMap.put("designation", "Principal Engineer");
        JasperPrint jasperPrint = JasperFillManager.fillReport(report, templateMap, new JREmptyDataSource());
        generateImage("D:\\visiting_card.png", jasperPrint);
    }

    private static void generateImage(String outputImagePath, JasperPrint print) {
        File file = new File(outputImagePath);
        try (OutputStream out = new FileOutputStream(file)) {
            BufferedImage img = JasperReportsUtils.printToImage(print, 0, 1.0f);
            ImageIO.write(img, "png", out);
        } catch (Exception e) {
            log.error("Can't generate image", e);
        }
    }
}
