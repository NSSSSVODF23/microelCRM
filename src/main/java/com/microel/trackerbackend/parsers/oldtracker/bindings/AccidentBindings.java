package com.microel.trackerbackend.parsers.oldtracker.bindings;

import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccidentBindings implements TrackerTaskDataBindings{
    private String login;
    private String address;
    private String description;
    private String workReport;
    private String phone;
    private Wireframe wireframe;
}
