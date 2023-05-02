package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.storehouse.Stuff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StuffRepository extends JpaRepository<Stuff, Long>, JpaSpecificationExecutor<Stuff> {
}
