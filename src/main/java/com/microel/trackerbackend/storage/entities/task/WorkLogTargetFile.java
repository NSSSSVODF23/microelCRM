package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.services.filemanager.FileSaver;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.*;

import javax.persistence.*;
import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "work_log_target_files")
public class WorkLogTargetFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workLogTargetFileId;
    private String name;
    private String mimeType;
    @Enumerated(EnumType.STRING)
    private FileType type;
    @Column(length = 4096)
    @JsonIgnore
    private String path;
    private Long size;
    @Column(name = "created")
    private Timestamp createdAt;
    @Nullable
    @Column(length = 4096)
    private String thumbnail;

    public static WorkLogTargetFile of(FileData fileData) throws EmptyFile, WriteError {
        fileData.setName(UUID.randomUUID()+"_"+ fileData.getName());
        FileSaver fileSaver = new FileSaver("work-log-targets");
        FileSaver.ComplexPath path = fileSaver.save(fileData);
        WorkLogTargetFile file = new WorkLogTargetFile();
        file.setName(fileData.getName());
        file.setMimeType(fileData.getType());
        file.setType(FileSaver.getFileType(fileData));
        file.setPath(path.getOriginal());
        file.setSize((long) fileData.getData().length);
        file.setCreatedAt(Timestamp.from(Instant.now()));
        file.setThumbnail(path.getThumbnail());
        return file;
    }

    @JsonIgnore
    public InputFile getInputFile() {
        return new InputFile(new File(getPath()));
    }

    @JsonIgnore
    public InputMedia toInputMedia(){
        if (name == null || name.isBlank() || path == null || path.isBlank())
            throw new NullPointerException("Имя файла или путь до него пусты");
        return switch (type) {
            case PHOTO -> InputMediaPhoto.builder()
                    .media("attach://" + name)
                    .mediaName(name)
                    .isNewMedia(true)
                    .newMediaFile(new File(path))
                    .parseMode(ParseMode.HTML)
                    .build();
            case VIDEO -> InputMediaVideo.builder()
                    .media("attach://" + name)
                    .mediaName(name)
                    .isNewMedia(true)
                    .newMediaFile(new File(path))
                    .parseMode(ParseMode.HTML)
                    .build();
            case AUDIO -> InputMediaAudio.builder()
                    .media("attach://" + name)
                    .mediaName(name)
                    .isNewMedia(true)
                    .newMediaFile(new File(path))
                    .parseMode(ParseMode.HTML)
                    .build();
            case DOCUMENT, FILE -> InputMediaDocument.builder()
                    .media("attach://" + name)
                    .mediaName(name)
                    .isNewMedia(true)
                    .newMediaFile(new File(path))
                    .parseMode(ParseMode.HTML)
                    .build();
            default -> null;
        };
    }
}
