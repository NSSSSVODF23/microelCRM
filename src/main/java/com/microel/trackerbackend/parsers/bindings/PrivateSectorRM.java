package com.microel.trackerbackend.parsers.bindings;

import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrivateSectorRM implements TrackerTaskDataBindings{
    private String gardening;
    private String address;
    private String name;
    private String phone;
    private String advertisingSource;
    private Wireframe wireframe;
}
