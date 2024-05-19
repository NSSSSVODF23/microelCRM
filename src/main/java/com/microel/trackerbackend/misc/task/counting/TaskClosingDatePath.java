package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.misc.TimeFrame;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class TaskClosingDatePath extends TaskTypePath {
    protected TimeFrame dateOfClose;

    public static TaskClosingDatePath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType, TaskStatus taskStatus, Long taskClassId, String taskTypeId, TimeFrame dateOfClose) {
        TaskClosingDatePath path = new TaskClosingDatePath();
        path.setSchedulingType(schedulingType);
        path.setTaskStatus(taskStatus);
        path.setTaskClassId(taskClassId);
        path.setTaskTypeId(taskTypeId);
        path.setDateOfClose(dateOfClose);
        return path;
    }

    public String getDateOfClose() {return dateOfClose.getValue();}

    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        condition.setStatus(List.of(taskStatus));
        condition.setTemplate(taskClassId == null ? null : Set.of(taskClassId));
        condition.setStage(taskTypeId);
        condition.setDateOfClose(DateRange.of(dateOfClose));
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskClosingDatePath that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getDateOfClose(), that.getDateOfClose());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDateOfClose());
    }

    @Override
    public String toString() {
        return super.toString() + ", dateOfClose: " + getDateOfClose();
    }
}
