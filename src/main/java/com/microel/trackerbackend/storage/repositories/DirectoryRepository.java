package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.filesys.Directory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DirectoryRepository extends JpaRepository<Directory, Long>, JpaSpecificationExecutor<Directory> {
    Optional<Directory> findFirstByPath(String path);
}
