package com.microel.trackerbackend.storage.dto.task;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TaskGroupDto {
    private Long groupId;
    private TaskGroupDto parent;
    private String name;
    private String description;
}
