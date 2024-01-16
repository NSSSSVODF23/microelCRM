package com.microel.trackerbackend.misc.task.counting;

import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;

public interface AbstractTaskCounterPath {
    TaskDispatcher.FiltrationConditions toFiltrationCondition();
}
