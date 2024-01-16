package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.entities.comments.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentListDto {
    private Long commentId;
    private Timestamp created;
    private EmployeeListDto creator;
    private String simpleText;

    @Nullable
    public static CommentListDto of(@Nullable Comment comment) {
        if(comment == null) return null;
        return CommentListDto.builder()
                .commentId(comment.getCommentId())
                .created(comment.getCreated())
                .creator(EmployeeListDto.of(comment.getCreator()))
                .simpleText(comment.getSimpleText())
                .build();
    }
}
