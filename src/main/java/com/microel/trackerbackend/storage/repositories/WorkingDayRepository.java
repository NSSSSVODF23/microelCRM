package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkingDayRepository extends JpaRepository<WorkingDay, Long>, JpaSpecificationExecutor<WorkingDay> {
}
