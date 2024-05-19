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
public class TaskTermDatePath extends TaskTypePath {
    protected TimeFrame actualTo;

    public static TaskTermDatePath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus, Long taskClassId, String taskTypeId, TimeFrame actualTo) {
        TaskTermDatePath path = new TaskTermDatePath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        path.setTaskTypeId(taskTypeId);
        path.setActualTo(actualTo);
        return path;
    }

    public String getActualTo() {return actualTo.getValue();}

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        condition.setStage(taskTypeId);
        condition.setActualTo(DateRange.of(actualTo));
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskTermDatePath that)) return false;
        if (!super.equals(o)) return false;
        return getActualTo() == that.getActualTo();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getActualTo());
    }

    @Override
    public String toString() {
        return super.toString() + ", actualTo=" + getActualTo();
    }
}
