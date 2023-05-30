package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.comments.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {
    Page<Comment> findAllByParent_TaskIdAndDeletedIsFalse(Long taskId, Pageable pageable);
}
