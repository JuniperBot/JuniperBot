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
package ru.juniperbot.common.model.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamAppDetails implements Serializable {
    private static final long serialVersionUID = -1557841368928963623L;

    private String name;

    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("header_image")
    private String headerImage;

    @JsonProperty("website")
    private String webSite;

    private List<String> developers;

    private List<String> publishers;

    @JsonProperty("price_overview")
    private SteamAppPrice price;

    private SteamAppPlatforms platforms;

    @JsonProperty("release_date")
    private SteamAppReleaseDate releaseDate;

    private List<SteamAppScreenshot> screenshots;
}
