package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.misc.TimeFrame;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class TaskScheduleDatePath extends TaskTypePath {
    protected TimeFrame actualFrom;

    public static TaskScheduleDatePath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus, Long taskClassId, String taskTypeId, TimeFrame actualFrom) {
        TaskScheduleDatePath path = new TaskScheduleDatePath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        path.setTaskTypeId(taskTypeId);
        path.setActualFrom(actualFrom);
        return path;
    }

    public String getActualFrom() {return actualFrom.getValue();}

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        condition.setStage(taskTypeId);
        condition.setActualFrom(DateRange.of(actualFrom));
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskScheduleDatePath that)) return false;
        if (!super.equals(o)) return false;
        return getActualFrom() == that.getActualFrom();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getActualFrom());
    }

    @Override
    public String toString() {
        return super.toString() + ", actualFrom: " + getActualFrom();
    }
}
