package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TaskStageRepository extends JpaRepository<TaskStage, String>, JpaSpecificationExecutor<TaskStage> {
    Optional<TaskStage> findFirstByStageId(String stageId);
}
