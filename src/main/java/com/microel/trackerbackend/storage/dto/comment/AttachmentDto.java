package com.microel.trackerbackend.storage.dto.comment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import lombok.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;
import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AttachmentDto {
    private String name;
    private String mimeType;
    private FileType type;
    private String path;
    private Long size;
    private Timestamp createdAt;
    private Timestamp modifiedAt;
    private String thumbnail;

    @JsonIgnore
    public InputFile getInputFile() {
        return new InputFile(new File(path));
    }
}
