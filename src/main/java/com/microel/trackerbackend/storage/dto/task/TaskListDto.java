package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.dto.templating.TaskStageDto;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.templating.TaskTypeDirectory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskListDto {
    private Long taskId;
    private Timestamp created;
    private List<CommentListDto> lastComments;
    private EmployeeListDto creator;
    private Timestamp actualFrom;
    private Timestamp actualTo;
    private TaskStatus taskStatus;
    private Set<TaskTagListDto> tags;
    private WireframeListDto modelWireframe;
    private List<ModelItemDto> fields;
    private EmployeeListDto responsible;
    private TaskStageDto currentStage;
    private TaskTypeDirectory currentDirectory;
    @Nullable
    private Long oldTrackerTaskId;

    @Nullable
    public static TaskListDto of(@Nullable Task task) {
        if(task == null) return null;
        return TaskListDto.builder()
                .taskId(task.getTaskId())
                .created(task.getCreated())
                .lastComments(task.getLastComments().stream().map(CommentListDto::of).collect(Collectors.toList()))
                .creator(EmployeeListDto.of(task.getCreator()))
                .actualFrom(task.getActualFrom())
                .actualTo(task.getActualTo())
                .taskStatus(task.getTaskStatus())
                .tags(task.getTags().stream().map(TaskTagListDto::of).collect(Collectors.toSet()))
                .modelWireframe(WireframeListDto.of(task.getModelWireframe()))
                .fields(task.getFields().stream().map(ModelItemDto::of).toList())
                .responsible(EmployeeListDto.of(task.getResponsible()))
                .currentStage(TaskStageDto.of(task.getCurrentStage()))
                .currentDirectory(task.getCurrentDirectory())
                .oldTrackerTaskId(task.getOldTrackerTaskId())
                .build();
    }
}
