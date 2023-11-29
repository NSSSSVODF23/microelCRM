package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.filesys.TFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface FileRepository extends JpaRepository<TFile, Long>, JpaSpecificationExecutor<TFile> {
    Optional<TFile> findFirstByPath(String path);
}
