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
package ru.caramel.juniperbot.web.utils;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.*;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FeedUtils {

    private FeedUtils() {
        // private class
    }

    public static List<Video> parseVideos(SyndFeed feed) {
        if (feed == null) {
            return Collections.emptyList();
        }

        return feed.getEntries().stream().map(e -> {
            SyndFeed self = feed;

            String channelId = getForeignValue(e, "channelId");
            if (StringUtils.isEmpty(channelId)) {
                throw new IllegalArgumentException("Wrong feed format, no channel id found");
            }

            String videoId = getForeignValue(e, "videoId");
            if (StringUtils.isEmpty(videoId)) {
                throw new IllegalArgumentException("Wrong feed format, no videoId found");
            }

            String channelTitle = CollectionUtils.isNotEmpty(e.getAuthors()) ? e.getAuthors().get(0).getName() : null;
            if (StringUtils.isEmpty(channelId)) {
                throw new IllegalArgumentException("Wrong feed format, no channel name found");
            }

            String videoTitle = e.getTitle();
            if (StringUtils.isEmpty(videoTitle)) {
                throw new IllegalArgumentException("Wrong feed format, no videoTitle found");
            }

            Element group = getForeignValue(e.getForeignMarkup(), "group");
            if (group == null) {
                throw new IllegalArgumentException("Wrong feed format, no group found");
            }

            VideoSnippet snippet = new VideoSnippet()
                    .setChannelId(channelId)
                    .setChannelTitle(channelTitle)
                    .setTitle(videoTitle);

            Video video = new Video()
                    .setId(videoId)
                    .setSnippet(snippet);

            Element thumbnailNode = group.getChild("thumbnail", group.getNamespace());
            if (thumbnailNode != null) {
                Thumbnail thumbnail = new Thumbnail()
                        .setUrl(thumbnailNode.getAttributeValue("url"))
                        .setWidth(Long.parseLong(thumbnailNode.getAttributeValue("width")))
                        .setHeight(Long.parseLong(thumbnailNode.getAttributeValue("height")));
                snippet.setThumbnails(new ThumbnailDetails().setDefault(thumbnail));
            }

            Element descriptionNode = group.getChild("description", group.getNamespace());
            if (descriptionNode != null) {
                snippet.setDescription(descriptionNode.getValue());
            }

            Element communityNode = group.getChild("community", group.getNamespace());
            if (communityNode != null) {
                Element statisticsNode = communityNode.getChild("statistics", group.getNamespace());
                if (statisticsNode != null && statisticsNode.getAttributeValue("views") != null) {
                    Long views = Long.parseLong(statisticsNode.getAttributeValue("views"));
                    video.setStatistics(new VideoStatistics()
                            .setViewCount(BigInteger.valueOf(views)));
                }
            }

            if (e.getPublishedDate() != null) {
                snippet.setPublishedAt(new DateTime(e.getPublishedDate()));
            }

            return video;
        }).collect(Collectors.toList());
    }

    public static String getForeignValue(SyndEntry entry, String name) {
        if (entry == null) {
            return null;
        }
        Element element = getForeignValue(entry.getForeignMarkup(), name);
        return element != null ? element.getValue() : null;
    }

    public static Element getForeignValue(List<Element> elements, String name) {
        if (CollectionUtils.isEmpty(elements)) {
            return null;
        }
        return elements.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
