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
package ru.juniperbot.module.steam.model.details;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamAppPrice implements Serializable {
    private static final long serialVersionUID = -32572487900411648L;

    private String currency;

    private Long initial;

    @JsonProperty("final")
    private Long finalPrice;

    @JsonProperty("discount_percent")
    private byte discountPercent;

    @JsonIgnore
    public boolean isCorrect() {
        return StringUtils.isNotEmpty(currency) && getPrice() != null;
    }

    @JsonIgnore
    public Long getPrice() {
        return finalPrice != null ? finalPrice : initial;
    }
}
