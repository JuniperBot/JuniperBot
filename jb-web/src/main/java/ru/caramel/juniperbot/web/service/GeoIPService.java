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
package ru.caramel.juniperbot.web.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class GeoIPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIPService.class);

    private static Set<String> RU_COUNTRIES = new HashSet<>(Arrays.asList("ru", "by", "kg", "kz", "md", "ua"));

    @Value("${geoip2.mmdb.location:}")
    private String mmdbPath;

    private DatabaseReader reader;

    @PostConstruct
    public void init() throws IOException {
        if (StringUtils.isNotEmpty(mmdbPath)) {
            File database = new File(mmdbPath);
            if (database.exists() && database.isFile()) {
                reader = new DatabaseReader.Builder(database).build();
            }
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }

    public String getLocale(HttpServletRequest request) throws IOException {
        return getLocale(getClientIp(request));
    }

    public String getLocale(String ip) throws IOException {
        return getLocale(InetAddress.getByName(ip));
    }

    public String getLocale(InetAddress ipAddress) throws IOException {
        if (reader != null) {
            try {
                CountryResponse countryResponse = reader.country(ipAddress);
                if (countryResponse != null &&
                        countryResponse.getCountry() != null &&
                        countryResponse.getCountry().getIsoCode() != null) {
                    if (RU_COUNTRIES.stream()
                            .anyMatch(e -> e.equals(countryResponse.getCountry().getIsoCode().toLowerCase()))) {
                        return "ru";
                    }
                }
            } catch (AddressNotFoundException e) {
                // ignore
            } catch (GeoIp2Exception e) {
                LOGGER.error("GeoIP2 getCountry error", e);
            }
        }
        return null;
    }
}
