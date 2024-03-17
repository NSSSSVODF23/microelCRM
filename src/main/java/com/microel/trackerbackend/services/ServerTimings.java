package com.microel.trackerbackend.services;

import net.time4j.PrettyTime;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServerTimings {
    ConcurrentHashMap<String, Instant> timings = new ConcurrentHashMap<>();

    public void start(String name) {
        timings.put(name, Instant.now());
    }

    public void stop(String name) {
        Instant instant = timings.remove(name);
        if (instant != null) {
            Duration duration = Duration.between(instant, Instant.now());
            System.out.println(name + ": " + duration.toMillis() + " ms");
        }
    }
}
