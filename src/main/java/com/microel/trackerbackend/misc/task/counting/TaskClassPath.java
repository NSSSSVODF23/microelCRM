package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class TaskClassPath extends TaskStatusPath {
    protected Long taskClassId;

    public static TaskClassPath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus, Long taskClassId) {
        TaskClassPath path = new TaskClassPath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        return path;
    }

    public String getTaskClassId() {
        return taskClassId.toString();
    }

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        return condition;
    }
}
