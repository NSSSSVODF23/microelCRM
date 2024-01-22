package com.microel.trackerbackend.services.external.billing.directaccess;

import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Url {
    public static String create(String... fragment){
        return Stream.of(fragment).filter(Objects::nonNull).collect(Collectors.joining("/"));
    }
    public static String create(@Nullable Map<String,String> query, String... fragment){
        String url = Stream.of(fragment).filter(Objects::nonNull).collect(Collectors.joining("/"));
        if (query == null || query.isEmpty()) return url;
        return url + "?" + query.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
    }
}
