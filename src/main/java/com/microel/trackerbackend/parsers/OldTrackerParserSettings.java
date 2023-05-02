package com.microel.trackerbackend.parsers;

import com.microel.trackerbackend.controllers.configuration.AbstractConfiguration;
import com.microel.trackerbackend.parsers.bindings.BindingsCollection;
import com.microel.trackerbackend.parsers.bindings.TrackerTaskDataBindings;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class OldTrackerParserSettings implements AbstractConfiguration {
    private Integer startId = 0;
    private Integer endId = 0;
    private String trackerLogin = "";
    private String trackerPassword = "";
    private String trackerUrl = "http://tracker.vdonsk.ru";
    private BindingsCollection bindings;
}
