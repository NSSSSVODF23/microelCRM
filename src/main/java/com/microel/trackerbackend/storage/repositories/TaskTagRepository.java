package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskTagRepository extends JpaRepository<TaskTag, Long>, JpaSpecificationExecutor<TaskTag> {
    TaskTag findByName(String name);

    List<TaskTag> findAllByDeletedIsFalse();
}
