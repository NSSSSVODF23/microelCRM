package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.repositories.TaskEventRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
public class TaskEventDispatcher {
    private final TaskEventRepository taskEventRepository;

    public TaskEventDispatcher(TaskEventRepository taskEventRepository) {
        this.taskEventRepository = taskEventRepository;
    }

    public List<TaskEvent> getTaskEventsTo(Long taskId, Timestamp to) {
        return taskEventRepository.findAllByTask_TaskIdAndCreatedGreaterThan(taskId, to);
    }

    public List<TaskEvent> getTaskEventsFrom(Long taskId, Timestamp from) {
        return taskEventRepository.findAllByTask_TaskIdAndCreatedLessThan(taskId, from);
    }

    public List<TaskEvent> getTaskEvents(Long id) {
        return taskEventRepository.findAllByTask_TaskId(id);
    }

    public List<TaskEvent> getTaskEvents(Long id, Timestamp from, Timestamp to) {
        return taskEventRepository.findAllByTask_TaskIdAndCreatedLessThanAndCreatedGreaterThan(id, from, to);
    }

    public TaskEvent appendEvent(TaskEvent event) {
        return taskEventRepository.save(event);
    }
}
