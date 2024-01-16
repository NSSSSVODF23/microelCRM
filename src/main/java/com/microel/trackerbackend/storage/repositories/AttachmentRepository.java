package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.comments.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AttachmentRepository extends JpaRepository<Attachment, String>, JpaSpecificationExecutor<Attachment> {
}
