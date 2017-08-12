package ru.caramel.juniperbot.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

public class DiscordHttpRequestFactory extends SimpleClientHttpRequestFactory {

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        ClientHttpRequest request = super.createRequest(uri, httpMethod);
        HttpHeaders headers = request.getHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:33.0) Gecko/20100101 Firefox/33.0");
        headers.add("Connection", "keep-alive");
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
