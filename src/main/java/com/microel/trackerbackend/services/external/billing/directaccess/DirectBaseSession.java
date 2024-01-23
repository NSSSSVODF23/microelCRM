package com.microel.trackerbackend.services.external.billing.directaccess;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.team.util.Credentials;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.CookieStore;
import java.util.Map;

public class DirectBaseSession {
    private String host;
    private Connection session = Jsoup.newSession();
    private Credentials credentials;

    protected DirectBaseSession(String host, Credentials credentials) {
        setHost(host);
        setCredentials(credentials);
    }

    protected String getHost(){
        if(host == null) throw new ResponseException("Host not set");
        return host;
    }

    private void setHost(String host) {
        this.host = host;
    }

    protected Credentials getCredentials() {
        return credentials;
    }

    protected void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    protected Connection.Response request(Request request) throws IOException {
        String endpoint = request.getEndpoint();
        Map<String, String> query = request.getQuery();
        Map<String, String> body = request.getBody();
        Connection.Method method = request.getMethod();

        String url = Url.create(query, getHost(), endpoint);
        Connection connection = session.newRequest().url(url).method(method);
        if(body != null && !body.isEmpty()) connection.data(body);
        connection.timeout(60000);
//        connection.ignoreContentType(true);
        return connection.execute();
    }
}
