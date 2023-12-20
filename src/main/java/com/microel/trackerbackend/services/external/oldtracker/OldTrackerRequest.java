package com.microel.trackerbackend.services.external.oldtracker;

import java.util.Map;

public interface OldTrackerRequest<T> {
    Map<String, String> getHeaders();
    Map<String, String> getCookies();
    T execute();
}
