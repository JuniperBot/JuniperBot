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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;
import ru.juniperbot.module.render.utils.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class ImagingServiceImpl implements ImagingService {

    private static int AVATAR_SIZE = 256;

    private Image statusMask = null;

    private Cache<String, BufferedImage> images = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .build();

    @Override
    public BufferedImage getAvatar(User user) {
        // TODO implement some filesystem cache storage?
        try (BufferedInputStream in = new BufferedInputStream(new URL(user.getEffectiveAvatarUrl() + "?v=256").openStream())) {
            return ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public BufferedImage getAvatarWithStatus(Member member) {
        BufferedImage avatar = getAvatar(member.getUser());
        if (avatar == null) {
            return null;
        }

        BufferedImage result = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        if (avatar.getWidth() != AVATAR_SIZE || avatar.getHeight() != AVATAR_SIZE) {
            Image scaled = avatar.getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);
            g2d.drawImage(scaled, 0, 0, null);
        } else {
            g2d.drawImage(avatar, 0, 0, null);
        }

        Image mask = getMaskLayer();
        if (mask != null) {
            Composite defaultComposite = g2d.getComposite();
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
            g2d.setComposite(alphaComposite);
            g2d.drawImage(mask, 0, 0, null);
            g2d.setComposite(defaultComposite);
        }

        BufferedImage statusLayer = getStatusLayer(member);
        if (statusLayer != null) {
            g2d.drawImage(statusLayer, 0, 0, null);
        }

        g2d.dispose();
        return result;
    }

    private BufferedImage getStatusLayer(Member member) {
        if (member.getActivities().stream().anyMatch(e -> e.getType() == Activity.ActivityType.STREAMING)) {
            return getResourceImage("avatar-status-streaming.png");
        }
        return getResourceImage(String.format("avatar-status-%s.png", member.getOnlineStatus().getKey()));
    }

    private Image getMaskLayer() {
        if (statusMask == null) {
            synchronized (this) {
                if (statusMask == null) {
                    statusMask = ImageUtils.grayscaleToAlpha(getResourceImage("avatar-status-mask.png"));
                }
            }
        }
        return statusMask;
    }

    @Override
    public BufferedImage getResourceImage(String fileName) {
        try {
            return images.get(fileName, () -> {
                try (InputStream stream = this.getClass().getResourceAsStream("/images/" + fileName)) {
                    return ImageIO.read(stream);
                } catch (IOException e) {
                    log.warn("Couldn't load avatar status mask");
                    return null;
                }
            });
        } catch (ExecutionException e) {
            log.warn("Couldn't load resource image {}", fileName, e);
            return null;
        }
    }
}
