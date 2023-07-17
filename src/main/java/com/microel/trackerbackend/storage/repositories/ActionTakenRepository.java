package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.salary.ActionTaken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActionTakenRepository extends JpaRepository<ActionTaken, Long>, JpaSpecificationExecutor<ActionTaken> {
}
