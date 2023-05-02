package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.dto.comment.TaskEventDto;
import com.microel.trackerbackend.storage.dto.team.DepartmentDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.dto.templating.TaskStageDto;
import com.microel.trackerbackend.storage.dto.templating.WireframeDto;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TaskDto {
    private Long taskId;
    private Timestamp created;
    private Timestamp updated;
    private CommentDto lastComment;
    private EmployeeDto creator;
    private Timestamp actualFrom;
    private Timestamp actualTo;
    private TaskStatus taskStatus;
    private Boolean deleted = false;
    private Set<TaskTagDto> tags;
    private WireframeDto modelWireframe;
    private List<ModelItemDto> fields;
    private List<CommentDto> comments;
    private List<TaskEventDto> taskEvents;
    private EmployeeDto responsible;
    private List<EmployeeDto> employeesObservers;
    private List<DepartmentDto> departmentsObservers;
    private TaskStageDto currentStage;
    private TaskGroupDto group;
    private Long parent;
    private List<TaskDto> children;

}
