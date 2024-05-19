package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class TaskDirectoryPath extends TaskTypePath {
    protected Long taskDirectoryId;

    public static TaskDirectoryPath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType,TaskStatus taskStatus, Long taskClassId, String taskTypeId, Long taskDirectoryId) {
        TaskDirectoryPath path = new TaskDirectoryPath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        path.setTaskTypeId(taskTypeId);
        path.setTaskDirectoryId(taskDirectoryId);
        return path;
    }

    public String getTaskDirectoryId() {
        return taskDirectoryId.toString();
    }

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        condition.setStage(taskTypeId);
        condition.setDirectory(taskDirectoryId);
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDirectoryPath that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getTaskDirectoryId(), that.getTaskDirectoryId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTaskDirectoryId());
    }

    @Override
    public String toString() {
        return super.toString() + ", taskDirectoryId: " + taskDirectoryId;
    }
}
