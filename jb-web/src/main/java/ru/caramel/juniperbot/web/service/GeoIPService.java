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

    private static final String ATTR_X_FORWARDED_FOR = "X-FORWARDED-FOR";

    private static final String ATTR_CF_IP = "CF-Connecting-IP";

    private static final String ATTR_CF_GEO = "CF-IPCountry";

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
        if (StringUtils.isNotEmpty(remoteAddr = request.getHeader(ATTR_X_FORWARDED_FOR))) {
            return remoteAddr;
        }
        if (StringUtils.isNotEmpty(remoteAddr = request.getHeader(ATTR_CF_IP))) {
            return remoteAddr;
        }
        return request.getRemoteAddr();
    }

    public String getLocale(HttpServletRequest request) throws IOException {
        String locale = null;
        String cdLocale = request.getHeader(ATTR_CF_GEO);
        if (StringUtils.isNotEmpty(cdLocale)) {
            locale = getMatchingLocale(cdLocale.toLowerCase());
        }
        if (StringUtils.isEmpty(locale)) {
            locale = getLocale(getClientIp(request));
        }
        return locale;
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
                    return getMatchingLocale(countryResponse.getCountry().getIsoCode().toLowerCase());
                }
            } catch (AddressNotFoundException e) {
                // ignore
            } catch (GeoIp2Exception e) {
                LOGGER.error("GeoIP2 getCountry error", e);
            }
        }
        return null;
    }

    private String getMatchingLocale(String isoCode) {
        if (RU_COUNTRIES.stream().anyMatch(e -> e.equals(isoCode))) {
            return "ru";
        }
        return null;
    }
}
