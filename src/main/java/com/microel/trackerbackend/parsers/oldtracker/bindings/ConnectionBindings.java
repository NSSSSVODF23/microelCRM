package com.microel.trackerbackend.parsers.oldtracker.bindings;

import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionBindings implements TrackerTaskDataBindings {
    private String takenFrom;
    private String type;
    private String login;
    private String password;
    private String fullName;
    private String address;
    private String phone;
    private String advertisingSource;
    private String techInfo;
    private Wireframe wireframe;
}
