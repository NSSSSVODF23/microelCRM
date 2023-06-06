package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WorkLogRepository extends JpaRepository<WorkLog, Long>, JpaSpecificationExecutor<WorkLog> {
    Optional<WorkLog> findAllByTaskAndClosedIsNull(Task task);

    Optional<WorkLog> findAllByTask_TaskIdAndClosedIsNull(Long taskId);

    List<WorkLog> findAllByTask_TaskId(Long taskId, Sort created);

    Optional<WorkLog> findFirstByEmployees_TelegramUserIdAndClosedIsNull(String telegramUserId);

    List<WorkLog> findAllByClosedIsNull(Sort sort);

    List<WorkLog> findAllByClosedIsNullAndAcceptedEmployeesIsNotNull(Sort sort);

    Long countByClosedIsNull(Sort created);

    Optional<WorkLog> findFirstByTask_TaskIdAndClosedIsNull(Long taskId);
}
