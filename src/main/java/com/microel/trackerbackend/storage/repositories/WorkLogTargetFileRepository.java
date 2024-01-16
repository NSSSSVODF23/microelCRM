package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.WorkLogTargetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkLogTargetFileRepository extends JpaRepository<WorkLogTargetFile, Long>, JpaSpecificationExecutor<WorkLogTargetFile> {
}
