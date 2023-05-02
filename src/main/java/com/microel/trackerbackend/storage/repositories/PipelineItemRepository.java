package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.templating.pipeline.PipelineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PipelineItemRepository extends JpaRepository<PipelineItem, Long>, JpaSpecificationExecutor<PipelineItem> {
}
