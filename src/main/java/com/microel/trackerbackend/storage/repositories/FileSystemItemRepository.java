package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.filesys.FileSystemItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Set;

public interface FileSystemItemRepository extends JpaRepository<FileSystemItem, Long>, JpaSpecificationExecutor<FileSystemItem> {
    List<FileSystemItem> findAllByParentIsNull();
    List<FileSystemItem> findAllByParent_FileSystemItemId(Long id);

    void deleteAllByFileSystemItemIdIn(Set<Long> idToDelete);
}
