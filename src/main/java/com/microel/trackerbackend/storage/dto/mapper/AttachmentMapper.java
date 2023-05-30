package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.comment.AttachmentDto;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttachmentMapper {
    @Nullable
    public static AttachmentDto toDto(@Nullable Attachment attachment) {
        if(attachment == null) return null;
        return AttachmentDto.builder()
                .created(attachment.getCreated())
                .mimeType(attachment.getMimeType())
                .modified(attachment.getModified())
                .name(attachment.getName())
                .path(attachment.getPath())
                .size(attachment.getSize())
                .type(attachment.getType())
                .thumbnail(attachment.getThumbnail())
                .build();
    }
@Nullable
    public static Attachment fromDto(@Nullable AttachmentDto attachment) {
        if(attachment == null) return null;
        return Attachment.builder()
                .created(attachment.getCreated())
                .mimeType(attachment.getMimeType())
                .modified(attachment.getModified())
                .name(attachment.getName())
                .path(attachment.getPath())
                .size(attachment.getSize())
                .type(attachment.getType())
                .thumbnail(attachment.getThumbnail())
                .build();
    }
}
