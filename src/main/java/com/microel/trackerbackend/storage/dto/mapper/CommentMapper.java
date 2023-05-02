package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class CommentMapper {
    @Nullable
    public static CommentDto toDto(@Nullable Comment comment) {
        if (comment == null) return null;
        return CommentDto.builder()
                .commentId(comment.getCommentId())
                .created(comment.getCreated())
                .creator(EmployeeMapper.toDto(comment.getCreator()))
                .message(comment.getMessage())
                .attachments(comment.getAttachments() == null ? new ArrayList<>() : comment.getAttachments().stream().map(AttachmentMapper::toDto).collect(Collectors.toList()))
                .replyComment(CommentMapper.toDto(comment.getReplyComment()))
                .deleted(comment.getDeleted())
                .edited(comment.getEdited())
                .build();
    }

    @Nullable
    public static Comment fromDto(@Nullable CommentDto comment) {
        if (comment == null) return null;
        return Comment.builder()
                .commentId(comment.getCommentId())
                .created(comment.getCreated())
                .creator(EmployeeMapper.fromDto(comment.getCreator()))
                .message(comment.getMessage())
                .attachments(comment.getAttachments() == null ? new ArrayList<>() : comment.getAttachments().stream().map(AttachmentMapper::fromDto).collect(Collectors.toList()))
                .replyComment(CommentMapper.fromDto(comment.getReplyComment()))
                .deleted(comment.getDeleted())
                .edited(comment.getEdited())
                .build();
    }
}
