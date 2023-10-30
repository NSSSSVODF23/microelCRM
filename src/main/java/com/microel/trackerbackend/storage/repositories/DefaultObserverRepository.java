package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DefaultObserverRepository extends JpaRepository<DefaultObserver, String>, JpaSpecificationExecutor<DefaultObserver> {
}
