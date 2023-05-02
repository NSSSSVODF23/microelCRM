package com.microel.trackerbackend.storage.dto.comment;

import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AttachmentDto {
    private String name;
    private String mimeType;
    private AttachmentType type;
    private String path;
    private Long size;
    private Timestamp created;
    private Timestamp modified;
    private List<CommentDto> comments;
}
