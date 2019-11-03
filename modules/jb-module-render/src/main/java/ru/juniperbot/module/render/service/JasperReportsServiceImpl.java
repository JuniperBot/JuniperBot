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
package ru.juniperbot.module.render.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fonts.FontUtil;
import org.springframework.stereotype.Service;
import ru.juniperbot.module.render.model.ReportType;
import ru.juniperbot.module.render.utils.ImageUtils;
import ru.juniperbot.module.render.utils.JasperReportsUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class JasperReportsServiceImpl implements JasperReportsService {

    private final Map<ReportType, JasperReport> reports = new ConcurrentHashMap<>();

    private final static List<String> FALLBACK_FONTS = List.of(
            "DejaVu Sans",
            "Free Serif",
            "Tibetan Machine Uni",
            "Noto CJK JP",
            "Symbola");

    @Override
    public BufferedImage generateImage(ReportType type, Map<String, Object> templateMap) {
        try {
            JasperReport report = loadReport(type);
            JasperPrint print = JasperFillManager.fillReport(report, templateMap, new JREmptyDataSource());
            return JasperReportsUtils.printToImage(print, 0, 1.0f);
        } catch (Throwable e) {
            log.error("Could not render report [{}] with map [{}]", type.name(), templateMap, e);
            return null;
        }
    }

    @Override
    public byte[] generateImageBytes(ReportType type, Map<String, Object> templateMap) {
        BufferedImage image = generateImage(type, templateMap);
        if (image == null) {
            return null;
        }
        return ImageUtils.getImageBytes(image, "png");
    }

    @Override
    public List<Font> loadFontVariants(int style, float size, String... names) {
        FontUtil util = FontUtil.getInstance(DefaultJasperReportsContext.getInstance());
        List<Font> result = new ArrayList<>(names.length + FALLBACK_FONTS.size());

        List<String> targetNames = new ArrayList<>(names.length + FALLBACK_FONTS.size());
        targetNames.addAll(Arrays.asList(names));
        targetNames.addAll(FALLBACK_FONTS);

        for (String name : targetNames) {
            Font font = util.getAwtFontFromBundles(true, name, style, size, null, true);
            if (font != null) {
                result.add(font);
            }
        }
        return result;
    }

    private JasperReport loadReport(ReportType type) throws JRException {
        try {
            return reports.computeIfAbsent(type, reportType -> {
                try (InputStream reportStream = JasperReportsServiceImpl.class.getResourceAsStream(type.getPath())) {
                    return JasperCompileManager.compileReport(reportStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof JRException) {
                throw (JRException) e.getCause();
            }
            throw e;
        }
    }
}
