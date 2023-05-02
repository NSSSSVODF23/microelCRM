package com.microel.trackerbackend.storage.entities.comments.dto;

import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.TaskJournalItem;
import com.microel.trackerbackend.storage.entities.team.Employee;

import java.sql.Timestamp;
import java.util.List;

public interface CommentDto extends TaskJournalItem {
    Long getCommentId();

    String getMessage();

    Timestamp getCreated();

    Employee getCreator();

    List<Attachment> getAttachments();

    CommentDto getReplyComment();

    Boolean getEdited();

    Boolean getDeleted();
}
