package com.microel.trackerbackend.storage.dto.comment;

import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEventType;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TaskEventDto {
    private Long taskEventId;
    private TaskEventType type;
    private String message;
    private EmployeeDto creator;
    private Timestamp created;
    private TaskDto task;
}
