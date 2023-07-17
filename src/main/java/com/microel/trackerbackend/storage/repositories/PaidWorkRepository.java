package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaidWorkRepository extends JpaRepository<PaidWork, Long>, JpaSpecificationExecutor<PaidWork> {
}
