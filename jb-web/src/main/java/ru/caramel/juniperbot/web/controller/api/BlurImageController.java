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
package ru.caramel.juniperbot.web.controller.api;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.web.service.OpenCVService;
import ru.caramel.juniperbot.web.utils.BoxBlurFilter;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@RestController
public class BlurImageController extends BaseRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlurImageController.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    private static final String[] ALLOWED_PREFIX = new String[]{
            "https://cdn.discordapp.com/icons/",
            "https://juniperbot.ru/resources/img/"
    };

    private BoxBlurFilter blurFilter;

    @Autowired
    private OpenCVService openCVService;

    @Value("${blur.image.opencv:true}")
    private boolean useOpenCV;

    @Value("${blur.image.opencv.radius:201}")
    private int blurRadius;

    @Value("${blur.image.cache:}")
    private String cacheFolder;

    private static class ImageInfo {
        private InputStream inputStream;
        private long contentLength;
    }

    @PostConstruct
    public void init() {
        blurFilter = new BoxBlurFilter(75);
        try {
            getCache();
        } catch (IOException e) {
            LOGGER.error("Could not create blur cache folder", e);
        }
    }

    @RequestMapping(value = "/blur", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> blur(@RequestParam("source") String sourceUrl, HttpServletResponse response) {
        if (!StringUtils.startsWithAny(sourceUrl, ALLOWED_PREFIX)) {
            return ResponseEntity.badRequest().build();
        }

        String hash = DigestUtils.sha1Hex(sourceUrl);

        try {
            ImageInfo info = readCached(hash);
            if (info == null) {
                info = renderImage(sourceUrl);
                if (info != null) {
                    saveCached(hash, info);
                }
            }
            if (info != null) {
                return ResponseEntity.ok()
                        .contentLength(info.contentLength)
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new InputStreamResource(info.inputStream));
            }
        } catch (IOException e) {
            // fall down
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    private ImageInfo readCached(String hash) throws IOException {
        File cacheDirectory = getCache();
        if (cacheDirectory == null) {
            return null;
        }

        File cachedImage = new File(cacheDirectory, hash + ".jpg");
        if (cachedImage.exists() && cachedImage.isFile()) {
            ImageInfo info = new ImageInfo();
            info.inputStream = new FileInputStream(cachedImage);
            info.contentLength = cachedImage.length();
            return info;
        }
        return null;
    }

    private void saveCached(String hash, ImageInfo info) throws IOException {
        File cacheDirectory = getCache();
        if (cacheDirectory == null) {
            return;
        }
        File cachedImage = new File(cacheDirectory, hash + ".jpg");
        FileUtils.copyInputStreamToFile(info.inputStream, cachedImage);
        info.inputStream.reset();
    }

    private File getCache() throws IOException {
        if (StringUtils.isEmpty(cacheFolder)) {
            return null;
        }
        File cacheDirectory = new File(cacheFolder);
        FileUtils.forceMkdir(cacheDirectory);
        return cacheDirectory;
    }

    private ImageInfo renderImage(String sourceUrl) throws MalformedURLException, IOException {
        URL url = new URL(sourceUrl);
        URLConnection con = url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);

        try (InputStream input = con.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            image = scaleImage(image, BufferedImage.TYPE_INT_RGB, 1920, 1920);
            BufferedImage blurredImage = null;
            if (useOpenCV && openCVService.isInitialized()) {
                try {
                    blurredImage = openCVService.blur(image, blurRadius);
                } catch (IOException e) {
                    // fall down and apply legacy box blur
                }
            }
            if (blurredImage == null) {
                blurredImage = blurFilter.filter(image, null);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(blurredImage, "jpg", out);

            ImageInfo info = new ImageInfo();
            info.inputStream = new ByteArrayInputStream(out.toByteArray());
            info.contentLength = out.size();
            return info;
        }
    }

    /**
     * @param image     The image to be scaled
     * @param imageType Target image type, e.g. TYPE_INT_RGB
     * @param newWidth  The required width
     * @param newHeight The required width
     * @return The scaled image
     */
    private static BufferedImage scaleImage(BufferedImage image, int imageType, int newWidth, int newHeight) {
        double thumbRatio = (double) newWidth / (double) newHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double aspectRatio = (double) imageWidth / (double) imageHeight;

        if (thumbRatio < aspectRatio) {
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newWidth = (int) (newHeight * aspectRatio);
        }

        BufferedImage newImage = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);
        return newImage;
    }
}
