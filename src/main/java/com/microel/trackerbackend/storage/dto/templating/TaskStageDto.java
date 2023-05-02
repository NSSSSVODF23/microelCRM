package com.microel.trackerbackend.storage.dto.templating;

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
}
