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
package ru.juniperbot.module.render.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.AttributedString;
import java.util.List;

@Slf4j
public final class ImageUtils {

    private ImageUtils() {
        // helper class
    }

    public static Image grayscaleToAlpha(BufferedImage image) {
        ImageFilter filter = new RGBImageFilter() {
            public final int filterRGB(int x, int y, int rgb) {
                return (rgb << 8) & 0xFF000000;
            }
        };

        ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    public static byte[] getImageBytes(@NonNull BufferedImage image, String format) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, format, os);
            return os.toByteArray();
        } catch (IOException e) {
            log.warn("Could not render image", e);
            return null;
        }
    }

    public static AttributedString createFallbackString(String text, List<Font> variants) {
        AttributedString result = new AttributedString(text);

        int textLength = text.length();

        Font currentFont = variants.get(0);
        result.addAttribute(TextAttribute.FONT, currentFont, 0, textLength);

        if (variants.size() > 1) {
            int fallbackBegin = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                Integer codePoint = null;
                if (Character.isHighSurrogate(c)) {
                    if (i < text.length() - 1) {
                        char c2 = text.charAt(i + 1);
                        if (Character.isLowSurrogate(c2)) {
                            codePoint = Character.toCodePoint(c, c2);
                        }
                    }
                }

                final Integer finalCodePoint = codePoint;
                Font fallback = variants.stream()
                        .filter(e -> finalCodePoint != null ? e.canDisplay(finalCodePoint) : e.canDisplay(c))
                        .findFirst()
                        .orElse(variants.get(0));
                if (currentFont != fallback) {
                    if (i > 0) {
                        result.addAttribute(TextAttribute.FONT, currentFont, fallbackBegin, i);
                    }
                    fallbackBegin = i;
                    currentFont = fallback;
                }

                int charWidth = finalCodePoint != null ? 2 : 1;
                if (i == text.length() - charWidth) {
                    result.addAttribute(TextAttribute.FONT, currentFont, fallbackBegin, i + charWidth);
                    break;
                }

                if (codePoint != null) {
                    i++;
                }
            }
        }
        return result;
    }
}
