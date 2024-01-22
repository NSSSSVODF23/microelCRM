package com.microel.trackerbackend.services.external.billing.directaccess;

import lombok.Getter;
import org.jsoup.Connection;
import org.springframework.lang.Nullable;

import java.util.Map;

@Getter
public class Request {
    @Nullable
    private String endpoint;
    @Nullable
    private Map<String, String> query;
    @Nullable
    private Map<String, String> body;
    private Connection.Method method;

    public static Request of(@Nullable String endpoint, @Nullable Map<String, String> query, @Nullable Map<String, String> body, @Nullable Connection.Method method) {
        Request request = new Request();
        request.endpoint = endpoint;
        request.query = query;
        request.body = body;
        request.method = method != null ? method : Connection.Method.GET;
        return request;
    }

    public static Request of(String endpoint, Map<String, String> query, Connection.Method method) {
        return of(endpoint, query, null, method);
    }

    public static Request of(String endpoint, Map<String, String> query) {
        return of(endpoint, query, null, null);
    }

    public static Request of(String endpoint) {
        return of(endpoint, null, null, null);
    }

    public static Request of() {
        return of(null, null, null, null);
    }

    public static Request of(Connection.Method method) {
        return of(null, null, null, method);
    }

    public static Request of(Map<String, String> body) {
        return of(null, null, body, null);
    }

    public static Request of(Map<String, String> body, Connection.Method method) {
        return of(null, null, body, method);
    }

    public static Request of(Map<String, String> query, Map<String, String> body) {
        return of(null, query, body, null);
    }

    public static Request of(String endpoint,Map<String, String> query, Map<String, String> body) {
        return of(endpoint, query, body, null);
    }

    public static Request of(Map<String, String> query, Map<String, String> body, Connection.Method method) {
        return of(null, query, body, method);
    }

    public static Request ofQuery(Map<String, String> query) {
        return of(null, query, null, null);
    }
}
