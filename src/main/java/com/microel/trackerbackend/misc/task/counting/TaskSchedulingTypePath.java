package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class TaskSchedulingTypePath implements AbstractTaskCounterPath {
    protected TaskDispatcher.FiltrationConditions.SchedulingType schedulingType;

    public static TaskSchedulingTypePath of(TaskDispatcher.FiltrationConditions.SchedulingType schedulingType) {
        TaskSchedulingTypePath path = new TaskSchedulingTypePath();
        path.setSchedulingType(schedulingType);
        return path;
    }

    public String getSchedulingType() {
        return schedulingType.getValue();
    }

    @Override
    public TaskDispatcher.FiltrationConditions toFiltrationCondition() {
        TaskDispatcher.FiltrationConditions condition = new TaskDispatcher.FiltrationConditions();
        condition.setSchedulingType(schedulingType);
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskSchedulingTypePath that)) return false;
        return Objects.equals(getSchedulingType(), that.getSchedulingType());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSchedulingType());
    }

    @Override
    public String toString() {
        return "Path scheduling type: " + getSchedulingType();
    }
}
