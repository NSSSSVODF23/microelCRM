package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.TaskGroupDto;
import com.microel.trackerbackend.storage.entities.task.utils.TaskGroup;
import org.springframework.lang.Nullable;

public class TaskGroupMapper {
    @Nullable
    public static TaskGroupDto toDto(@Nullable TaskGroup group) {
        if (group == null) return null;
        return TaskGroupDto.builder()
                .groupId(group.getGroupId())
                .description(group.getDescription())
                .name(group.getName())
                .parent(TaskGroupMapper.toDto(group.getParent()))
                .build();
    }

    @Nullable
    public static TaskGroup fromDto(@Nullable TaskGroupDto group) {
        if (group == null) return null;
        return TaskGroup.builder()
                .groupId(group.getGroupId())
                .description(group.getDescription())
                .name(group.getName())
                .parent(TaskGroupMapper.fromDto(group.getParent()))
                .build();
    }
}
