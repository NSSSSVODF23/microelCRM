package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.team.util.TelegramOptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TelegramOptionsRepository extends JpaRepository<TelegramOptions, Long>, JpaSpecificationExecutor<TelegramOptions> {
}
