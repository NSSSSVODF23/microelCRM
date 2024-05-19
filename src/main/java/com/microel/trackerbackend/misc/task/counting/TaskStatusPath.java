package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class TaskStatusPath extends TaskSchedulingTypePath {
    protected TaskStatus taskStatus;

    public static TaskStatusPath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus) {
        TaskStatusPath path = new TaskStatusPath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        return path;
    }

    public String getTaskStatus() {
        return taskStatus.name().toLowerCase();
    }

    @Override
    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskStatusPath that)) return false;
        return Objects.equals(getTaskStatus(), that.getTaskStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTaskStatus());
    }

    @Override
    public String toString() {
        return super.toString() + ", taskStatus: " + getTaskStatus();
    }
}
