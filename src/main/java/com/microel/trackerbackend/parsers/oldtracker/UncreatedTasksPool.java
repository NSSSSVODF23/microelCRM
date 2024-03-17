package com.microel.trackerbackend.parsers.oldtracker;

import com.microel.confstore.AbstractConfiguration;
import com.microel.trackerbackend.storage.dto.task.TaskDto;

import java.util.HashMap;
import java.util.UUID;

public class UncreatedTasksPool extends HashMap<UUID, TaskDto> implements AbstractConfiguration {

    @Override
    public String fileName() {
        return "uncreatedTasksPool.json";
    }

    @Override
    public Boolean isFilled() {
        return true;
    }
}
