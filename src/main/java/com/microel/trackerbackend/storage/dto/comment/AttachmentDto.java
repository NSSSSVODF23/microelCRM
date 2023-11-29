package com.microel.trackerbackend.storage.dto.comment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
import com.microel.trackerbackend.storage.exceptions.IllegalMediaType;
import lombok.*;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.*;

import java.io.File;
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
    private Timestamp createdAt;
    private Timestamp modifiedAt;
    private String thumbnail;

    @JsonIgnore
    public InputFile getInputFile() {
        return new InputFile(new File(path));
    }
}
