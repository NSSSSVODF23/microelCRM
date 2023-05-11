package com.microel.trackerbackend.parsers.oldtracker;

import com.microel.trackerbackend.controllers.configuration.AbstractConfiguration;
import com.microel.trackerbackend.storage.dto.task.TaskDto;

import java.util.HashMap;
import java.util.UUID;

public class UncreatedTasksPool extends HashMap<UUID, TaskDto> implements AbstractConfiguration {
}
