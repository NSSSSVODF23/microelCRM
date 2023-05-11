package com.microel.trackerbackend.parsers.oldtracker.bindings;

import com.microel.trackerbackend.storage.entities.templating.Wireframe;

public interface TrackerTaskDataBindings {
    Wireframe getWireframe();
    void setWireframe(Wireframe wireframe);
}
