package com.microel.trackerbackend.storage.dto.comment;

import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.comments.TaskJournalItem;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CommentDto {
    private Long commentId;
    private String message;
    private Timestamp created;
    private EmployeeDto creator;
    private List<AttachmentDto> attachments;
    private CommentDto replyComment;
    private Boolean edited;
    private Boolean deleted;
}
