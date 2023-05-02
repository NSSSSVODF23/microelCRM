package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.utils.TaskInitiator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskInitiatorRepository extends JpaRepository<TaskInitiator, Long>, JpaSpecificationExecutor<TaskInitiator> {
}
