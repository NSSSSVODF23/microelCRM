package com.microel.trackerbackend.storage.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTagListDto {
    private Long taskTagId;
    private String name;
    private String color;

    @Nullable
    public static TaskTagListDto of(@Nullable TaskTag taskTag) {
        if(taskTag == null) return null;
        return TaskTagListDto.builder()
                .taskTagId(taskTag.getTaskTagId())
                .name(taskTag.getName())
                .color(taskTag.getColor())
                .build();
    }
}
