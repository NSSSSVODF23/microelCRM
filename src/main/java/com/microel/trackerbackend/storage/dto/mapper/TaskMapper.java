package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.entities.task.Task;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class TaskMapper {
    @Nullable
    public static TaskDto toDto(@Nullable Task task) {
        if (task == null) return null;
        return TaskDto.builder()
                .creator(EmployeeMapper.toDto(task.getCreator()))
                .updated(task.getUpdated())
                .taskId(task.getTaskId())
                .actualFrom(task.getActualFrom())
                .actualTo(task.getActualTo())
                .children(task.getChildren() == null ? new ArrayList<>() : task.getChildren().stream().map(TaskMapper::toDto).collect(Collectors.toList()))
                .currentStage(TaskStageMapper.toDto(task.getCurrentStage()))
                .comments(task.getComments() == null ? new ArrayList<>() : task.getComments().stream().map(CommentMapper::toDto).collect(Collectors.toList()))
                .created(task.getCreated())
                .deleted(task.getDeleted())
                .departmentsObservers(task.getDepartmentsObservers() == null ? new ArrayList<>() : task.getDepartmentsObservers().stream().map(DepartmentMapper::toDto).collect(Collectors.toList()))
                .employeesObservers(task.getEmployeesObservers() == null ? new ArrayList<>() : task.getEmployeesObservers().stream().map(EmployeeMapper::toDto).collect(Collectors.toList()))
                .fields(task.getFields() == null ? new ArrayList<>() : task.getFields().stream().map(ModelItemMapper::toDto).collect(Collectors.toList()))
                .group(TaskGroupMapper.toDto(task.getGroup()))
                .modelWireframe(WireframeMapper.toDto(task.getModelWireframe()))
                .lastComment(CommentMapper.toDto(task.getLastComment()))
                .parent(task.getParent())
                .responsible(EmployeeMapper.toDto(task.getResponsible()))
                .tags(task.getTags() == null ? new HashSet<>() : task.getTags().stream().map(TaskTagMapper::toDto).collect(Collectors.toSet()))
                .taskEvents(task.getTaskEvents() == null ? new ArrayList<>() : task.getTaskEvents().stream().map(TaskEventMapper::toDto).collect(Collectors.toList()))
                .taskStatus(task.getTaskStatus())
                .listItemFields(task.getListItemFields())
                .build();
    }

    @Nullable
    public static TaskDto toListObject(@Nullable Task task){
        if (task == null) return null;
        return TaskDto.builder()
                .creator(EmployeeMapper.toDto(task.getCreator()))
                .updated(task.getUpdated())
                .taskId(task.getTaskId())
                .actualFrom(task.getActualFrom())
                .actualTo(task.getActualTo())
                .currentStage(TaskStageMapper.toDto(task.getCurrentStage()))
                .created(task.getCreated())
                .deleted(task.getDeleted())
                .fields(task.getFields() == null ? new ArrayList<>() : task.getFields().stream().map(ModelItemMapper::toDto).collect(Collectors.toList()))
                .modelWireframe(WireframeMapper.toDto(task.getModelWireframe()))
                .lastComment(CommentMapper.toDto(task.getLastComment()))
                .responsible(EmployeeMapper.toDto(task.getResponsible()))
                .tags(task.getTags() == null ? new HashSet<>() : task.getTags().stream().map(TaskTagMapper::toDto).collect(Collectors.toSet()))
                .taskStatus(task.getTaskStatus())
                .listItemFields(task.getListItemFields())
                .lastComment(CommentMapper.toDto(task.getLastComment()))
                .build();
    }

    @Nullable
    public static Task fromDto(@Nullable TaskDto task) {
        if (task == null) return null;
        Task build = Task.builder()
                .creator(EmployeeMapper.fromDto(task.getCreator()))
                .updated(task.getUpdated())
                .taskId(task.getTaskId())
                .actualFrom(task.getActualFrom())
                .actualTo(task.getActualTo())
                .currentStage(TaskStageMapper.fromDto(task.getCurrentStage()))
                .created(task.getCreated())
                .deleted(task.getDeleted())
                .departmentsObservers(task.getDepartmentsObservers() == null ? new ArrayList<>() : task.getDepartmentsObservers().stream().map(DepartmentMapper::fromDto).collect(Collectors.toList()))
                .employeesObservers(task.getEmployeesObservers() == null ? new ArrayList<>() : task.getEmployeesObservers().stream().map(EmployeeMapper::fromDto).collect(Collectors.toList()))
                .group(TaskGroupMapper.fromDto(task.getGroup()))
                .modelWireframe(WireframeMapper.fromDto(task.getModelWireframe()))
                .lastComment(CommentMapper.fromDto(task.getLastComment()))
                .parent(task.getParent())
                .responsible(EmployeeMapper.fromDto(task.getResponsible()))
                .taskEvents(task.getTaskEvents() == null ? new ArrayList<>() : task.getTaskEvents().stream().map(TaskEventMapper::fromDto).collect(Collectors.toList()))
                .taskStatus(task.getTaskStatus())
                .build();
        build.setTags(task.getTags() == null ? new HashSet<>() : task.getTags().stream().map(TaskTagMapper::fromDto).collect(Collectors.toSet()));
        build.setChildren(task.getChildren() == null ? new ArrayList<>() : task.getChildren().stream().map(TaskMapper::fromDto).collect(Collectors.toList()));
        build.setComments(task.getComments() == null ? new ArrayList<>() : task.getComments().stream().map(CommentMapper::fromDto).collect(Collectors.toList()));
        build.setFields(task.getFields() == null ? new ArrayList<>() : task.getFields().stream().map(ModelItemMapper::fromDto).collect(Collectors.toList()));
        return build;
    }
}
