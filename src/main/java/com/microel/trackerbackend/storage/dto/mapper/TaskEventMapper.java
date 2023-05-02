package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.comment.TaskEventDto;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import org.springframework.lang.Nullable;

public class TaskEventMapper {
    @Nullable
    public static TaskEventDto toDto(@Nullable TaskEvent taskEvent) {
        if (taskEvent == null) return null;
        return TaskEventDto.builder()
                .taskEventId(taskEvent.getTaskEventId())
                .task(TaskMapper.toDto(taskEvent.getTask()))
                .created(taskEvent.getCreated())
                .creator(EmployeeMapper.toDto(taskEvent.getCreator()))
                .message(taskEvent.getMessage())
                .type(taskEvent.getType())
                .build();
    }

    @Nullable
    public static TaskEvent fromDto(@Nullable TaskEventDto taskEvent) {
        if (taskEvent == null) return null;
        return TaskEvent.builder()
                .taskEventId(taskEvent.getTaskEventId())
                .task(TaskMapper.fromDto(taskEvent.getTask()))
                .created(taskEvent.getCreated())
                .creator(EmployeeMapper.fromDto(taskEvent.getCreator()))
                .message(taskEvent.getMessage())
                .type(taskEvent.getType())
                .build();
    }
}
