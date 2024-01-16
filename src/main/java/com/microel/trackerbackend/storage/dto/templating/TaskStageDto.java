package com.microel.trackerbackend.storage.dto.templating;

import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TaskStageDto {
    private String stageId;
    private String label;
    private Integer orderIndex;

    public static TaskStageDto of(TaskStage taskStage) {
        return TaskStageDto.builder()
                .stageId(taskStage.getStageId())
                .label(taskStage.getLabel())
                .orderIndex(taskStage.getOrderIndex())
                .build();
    }
}
