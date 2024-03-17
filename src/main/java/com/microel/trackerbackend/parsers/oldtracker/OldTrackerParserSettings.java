package com.microel.trackerbackend.parsers.oldtracker;

import com.microel.confstore.AbstractConfiguration;
import com.microel.trackerbackend.parsers.oldtracker.bindings.BindingsCollection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OldTrackerParserSettings implements AbstractConfiguration {
    private Integer startId = 0;
    private Integer endId = 0;
    private String trackerLogin = "";
    private String trackerPassword = "";
    private String trackerUrl = "http://tracker.vdonsk.ru";
    private BindingsCollection bindings;

    @Override
    public String fileName() {
        return "oldTracker.conf";
    }

    @Override
    public Boolean isFilled() {
        return true;
    }
}
