package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.utils.TaskGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long>, JpaSpecificationExecutor<TaskGroup> {
}
