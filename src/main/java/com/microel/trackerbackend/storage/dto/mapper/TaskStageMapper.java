package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.templating.TaskStageDto;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import org.springframework.lang.Nullable;

public class TaskStageMapper {
    @Nullable
    public static TaskStageDto toDto(@Nullable TaskStage taskStage) {
        if (taskStage == null) return null;
        return TaskStageDto.builder()
                .stageId(taskStage.getStageId())
                .label(taskStage.getLabel())
                .orderIndex(taskStage.getOrderIndex())
                .build();
    }

    @Nullable
    public static TaskStage fromDto(@Nullable TaskStageDto taskStage) {
        if (taskStage == null) return null;
        return TaskStage.builder()
                .stageId(taskStage.getStageId())
                .label(taskStage.getLabel())
                .orderIndex(taskStage.getOrderIndex())
                .build();
    }
}
