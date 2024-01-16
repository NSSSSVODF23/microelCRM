package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class TaskTypePath extends TaskClassPath {
    protected String taskTypeId;

    public static TaskTypePath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus, Long taskClassId, String taskTypeId) {
        TaskTypePath path = new TaskTypePath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        path.setTaskTypeId(taskTypeId);
        return path;
    }

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        condition.setStage(taskTypeId);
        return condition;
    }
}
