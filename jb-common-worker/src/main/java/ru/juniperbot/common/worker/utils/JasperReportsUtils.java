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
package ru.juniperbot.common.worker.utils;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.util.JRGraphEnvInitializer;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class JasperReportsUtils {

    private JasperReportsUtils() {
        // helper class
    }

    public static BufferedImage printToImage(JasperPrint jasperPrint, int pageIndex, float zoom) throws JRException {
        JRGraphEnvInitializer.initializeGraphEnv();
        PrintPageFormat pageFormat = jasperPrint.getPageFormat(pageIndex);

        int rasterWidth = (int) Math.ceil(pageFormat.getPageWidth() * zoom);
        int rasterHeight = (int) Math.ceil(pageFormat.getPageHeight() * zoom);
        BufferedImage pageImage = new BufferedImage(
                rasterWidth,
                rasterHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = (Graphics2D) pageImage.getGraphics();

        SimpleGraphics2DExporterOutput output = new SimpleGraphics2DExporterOutput();
        output.setGraphics2D(graphics);

        SimpleGraphics2DReportConfiguration configuration = new SimpleGraphics2DReportConfiguration();
        configuration.setPageIndex(pageIndex);
        configuration.setZoomRatio(zoom);
        configuration.setWhitePageBackground(false);

        JRGraphics2DExporter exporter = new JRGraphics2DExporter(DefaultJasperReportsContext.getInstance());
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(output);
        exporter.setConfiguration(configuration);
        exporter.exportReport();

        graphics.dispose();
        return pageImage;
    }
}
