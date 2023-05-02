package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskFieldsSnapshot;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskFieldsSnapshotRepository extends JpaRepository<TaskFieldsSnapshot, Long>, JpaSpecificationExecutor<TaskFieldsSnapshot> {
    List<TaskFieldsSnapshot> findByTask(Task task, Sort whenEdited);
}
