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
package ru.juniperbot.worker.common.shared.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static ru.juniperbot.common.utils.CommonUtils.HTTP_TIMEOUT;

public class DiscordHttpRequestFactory extends SimpleClientHttpRequestFactory {

    private final String token;

    public DiscordHttpRequestFactory(String token) {
        this.token = token;
        setConnectTimeout(HTTP_TIMEOUT);
        setReadTimeout(HTTP_TIMEOUT);
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        ClientHttpRequest request = super.createRequest(uri, httpMethod);
        HttpHeaders headers = request.getHeaders();
        headers.add("User-Agent", "JuniperBot DiscordBot (https://github.com/goldrenard/JuniperBot, 1.0)");
        headers.add("Connection", "keep-alive");
        headers.add("Authorization", "Bot " + token);
        return request;
    }

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod)
            throws IOException {
        super.prepareConnection(connection, httpMethod);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
    }
}
