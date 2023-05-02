package com.microel.trackerbackend.storage.entities.comments.dto;

import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CommentData {
    private Long taskId;
    private String text;
    private List<FileData> files;
    private Comment replyComment;
}
