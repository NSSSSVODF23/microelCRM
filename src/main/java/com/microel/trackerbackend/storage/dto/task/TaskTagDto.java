package com.microel.trackerbackend.storage.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import lombok.*;

import java.sql.Timestamp;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TaskTagDto {
    private Long taskTagId;
    private String name;
    private String color;
    private Boolean deleted;
    private Timestamp created;
    private EmployeeDto creator;
    @JsonIgnore
    private Set<TaskDto> task;
}
