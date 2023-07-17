package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PaidWorkGroupRepository extends JpaRepository<PaidWorkGroup, Long>, JpaSpecificationExecutor<PaidWorkGroup> {
}
