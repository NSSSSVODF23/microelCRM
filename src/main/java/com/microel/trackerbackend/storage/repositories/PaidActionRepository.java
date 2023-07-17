package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PaidActionRepository extends JpaRepository<PaidAction, Long>, JpaSpecificationExecutor<PaidAction> {
    boolean existsByNameIgnoreCase(String name);

    Optional<PaidAction> findByPaidActionIdAndDeletedFalse(Long id);

    List<PaidAction> findAllByDeletedFalseOrderByName();
}
