package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.team.util.OldTrackerCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OldTrackerCredentialsRepository extends JpaRepository<OldTrackerCredentials, Long>, JpaSpecificationExecutor<OldTrackerCredentials> {
}
