package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.Set;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long>, JpaSpecificationExecutor<WorkLog> {
    Optional<WorkLog> findAllByTaskAndClosedIsNull(Task task);

    Optional<WorkLog> findAllByTask_TaskIdAndClosedIsNull(Long taskId);

}
