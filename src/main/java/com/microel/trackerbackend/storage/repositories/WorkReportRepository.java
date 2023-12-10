package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.WorkReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkReportRepository extends JpaRepository<WorkReport, Long>, JpaSpecificationExecutor<WorkReport> {
}
