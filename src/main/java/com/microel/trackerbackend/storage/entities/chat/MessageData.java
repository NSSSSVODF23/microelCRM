package com.microel.trackerbackend.storage.entities.chat;

import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MessageData {
    private String text;
    private List<FileData> files;
    private Long replyMessageId;
}
