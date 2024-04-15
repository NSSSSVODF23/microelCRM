package com.microel.trackerbackend.storage.entities.comments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.comment.AttachmentDto;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.*;

import javax.persistence.*;
import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * Сущность базы данных представляющая собой вложение, хранит в себе метаданные файла сохраненного на диске.
 * На нее могут ссылаться комментарии к задаче и сообщения из чатов.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attachments")
public class Attachment {
    @Id
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
    @Column(name = "modified")
    private Timestamp modifiedAt;
    @Nullable
    @Column(length = 2048)
    private String thumbnail;
    @ManyToMany(mappedBy = "attachments")
    @JsonIgnore
    @BatchSize(size = 25)
    private List<Comment> comments;

    @JsonIgnore
    public InputFile getInputFile() {
        return new InputFile(new File(path));
    }

    @JsonIgnore
    @Nullable
    public static InputMedia getInputMedia(Attachment attachment) {
        String name = attachment.getName();
        FileType type = attachment.getType();
        String path = attachment.getPath();
        if (name == null || name.isBlank() || path == null || path.isBlank())
            throw new NullPointerException("Имя прикрепленного файла или путь до него пусты");
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

    @JsonIgnore
    @Nullable
    public static InputMedia getInputMedia(AttachmentDto attachment) {
        String name = attachment.getName();
        FileType type = attachment.getType();
        String path = attachment.getPath();
        if (name == null || name.isBlank() || path == null || path.isBlank())
            throw new NullPointerException("Имя прикрепленного файла или путь до него пусты");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attachment that)) return false;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
