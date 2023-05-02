package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.utils.ConnectedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConnectedServiceRepository extends JpaRepository<ConnectedService, Long>, JpaSpecificationExecutor<ConnectedService> {
}
