package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.sorting.TaskJournalSortingTypes;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.repositories.TaskEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
public class TaskEventDispatcher {
    private final TaskEventRepository taskEventRepository;

    public TaskEventDispatcher(TaskEventRepository taskEventRepository) {
        this.taskEventRepository = taskEventRepository;
    }

    public List<TaskEvent> getTaskEventsTo(Long taskId, Timestamp to, @Nullable TaskJournalSortingTypes sorting) {
        if (sorting == TaskJournalSortingTypes.CREATE_DATE_ASC) {
            return taskEventRepository.findAllByTask_TaskIdAndCreatedGreaterThan(taskId, to, Sort.by(Sort.Direction.ASC, "created"));
        }else{
            return taskEventRepository.findAllByTask_TaskIdAndCreatedGreaterThan(taskId, to, Sort.by(Sort.Direction.DESC, "created"));
        }
    }

    public List<TaskEvent> getTaskEventsFrom(Long taskId, Timestamp from, @Nullable TaskJournalSortingTypes sorting) {
        if (sorting == TaskJournalSortingTypes.CREATE_DATE_ASC) {
            return taskEventRepository.findAllByTask_TaskIdAndCreatedLessThan(taskId, from, Sort.by(Sort.Direction.ASC, "created"));
        }else{
            return taskEventRepository.findAllByTask_TaskIdAndCreatedLessThan(taskId, from, Sort.by(Sort.Direction.DESC, "created"));
        }
    }

    public List<TaskEvent> getTaskEvents(Long id, @Nullable TaskJournalSortingTypes sorting) {
        if (sorting == TaskJournalSortingTypes.CREATE_DATE_ASC) {
            return taskEventRepository.findAllByTask_TaskId(id, Sort.by( Sort.Direction.ASC,"created"));
        }
        return taskEventRepository.findAllByTask_TaskId(id, Sort.by(Sort.Direction.DESC, "created"));
    }

    public List<TaskEvent> getTaskEvents(Long id, Timestamp from, Timestamp to, @Nullable TaskJournalSortingTypes sorting) {
        if (sorting == TaskJournalSortingTypes.CREATE_DATE_ASC) {
            return taskEventRepository.findAllByTask_TaskIdAndCreatedLessThanAndCreatedGreaterThan(id, from, to, Sort.by(Sort.Direction.ASC, "created"));
        }
        return taskEventRepository.findAllByTask_TaskIdAndCreatedLessThanAndCreatedGreaterThan(id, from, to, Sort.by(Sort.Direction.DESC, "created"));
    }

    public TaskEvent appendEvent(TaskEvent event) {
        return taskEventRepository.save(event);
    }
}
