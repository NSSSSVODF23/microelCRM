package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.utils.AdSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdSourceRepository extends JpaRepository<AdSource, Long>, JpaSpecificationExecutor<AdSource> {
}
