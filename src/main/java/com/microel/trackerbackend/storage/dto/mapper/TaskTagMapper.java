package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.TaskTagDto;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import org.springframework.lang.Nullable;

public class TaskTagMapper {
    @Nullable
    public static TaskTagDto toDto(@Nullable TaskTag taskTag) {
        if (taskTag == null) return null;
        return TaskTagDto.builder()
                .taskTagId(taskTag.getTaskTagId())
                .name(taskTag.getName())
                .color(taskTag.getColor())
                .created(taskTag.getCreated())
                .creator(EmployeeMapper.toDto(taskTag.getCreator()))
                .deleted(taskTag.getDeleted())
                .build();
    }

    @Nullable
    public static TaskTag fromDto(@Nullable TaskTagDto taskTag) {
        if (taskTag == null) return null;
        return TaskTag.builder()
                .taskTagId(taskTag.getTaskTagId())
                .name(taskTag.getName())
                .color(taskTag.getColor())
                .created(taskTag.getCreated())
                .creator(EmployeeMapper.fromDto(taskTag.getCreator()))
                .deleted(taskTag.getDeleted())
                .build();
    }
}
