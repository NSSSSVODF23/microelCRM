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
public class TaskTagPath extends TaskDirectoryPath {
    private Long taskTagId;

    public static TaskTagPath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus, Long taskClassId, String taskTypeId, Long taskDirectoryId, Long taskTagId) {
        TaskTagPath path = new TaskTagPath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        path.setTaskTypeId(taskTypeId);
        path.setTaskDirectoryId(taskDirectoryId);
        path.setTaskTagId(taskTagId);
        return path;
    }

    public String getTaskTagId() {
        return taskTagId.toString();
    }

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        condition.setStage(taskTypeId);
        condition.setDirectory(taskDirectoryId);
        condition.setTags(taskTagId == null ? null : Set.of(taskTagId));
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskTagPath that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getTaskTagId(), that.getTaskTagId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTaskTagId());
    }

    @Override
    public String toString() {
        return super.toString() + ", taskTagId: " + getTaskTagId();
    }
}
