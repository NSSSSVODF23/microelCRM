package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.sql.Timestamp;
import java.util.List;

public interface TaskEventRepository extends JpaRepository<TaskEvent, Long>, JpaSpecificationExecutor<TaskEvent> {
    List<TaskEvent> findAllByTask_TaskIdAndCreatedLessThan(Long taskId, Timestamp to, Sort created);

    List<TaskEvent> findAllByTask_TaskId(Long id, Sort created);

    List<TaskEvent> findAllByTask_TaskIdAndCreatedGreaterThan(Long taskId, Timestamp from, Sort created);

    List<TaskEvent> findAllByTask_TaskIdAndCreatedLessThanAndCreatedGreaterThan(Long id, Timestamp from, Timestamp to, Sort created);
}
