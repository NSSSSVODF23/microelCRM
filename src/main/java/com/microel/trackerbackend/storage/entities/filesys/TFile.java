package com.microel.trackerbackend.storage.entities.filesys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.controllers.telegram.CallbackData;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import com.microel.trackerbackend.storage.entities.task.WorkLogTargetFile;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "file")
public class TFile extends FileSystemItem {
    @Nullable
    private String mimeType;

    public FileType getType() {
        if(mimeType == null) return FileType.FILE;
        String[] splitType = mimeType.split("\\/");
        switch (splitType[0]) {
            case "image":
                return FileType.PHOTO;
            case "video":
                return FileType.VIDEO;
            case "audio":
                return FileType.AUDIO;
            case "text":
                return FileType.DOCUMENT;
            case "application":
                if (Objects.equals(splitType[1], "pdf")) {
                    return FileType.DOCUMENT;
                }
                return FileType.FILE;
            default:
                return FileType.FILE;
        }
    }

    public Double getSizeMbyte() {
        return (double) getSize() / (1024d * 1024d);
    }

    @JsonIgnore
    public InputFile getInputFile() {
        return new InputFile(new File(getPath()));
    }

    public static TFile of(File file) {
        TFile tFile = new TFile();
        tFile.setName(file.getName());
        tFile.setPath(file.getPath());
        tFile.setSize(file.length());
        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            tFile.setCreatedAt(Timestamp.from(attr.creationTime().toInstant()));
            tFile.setModifiedAt(Timestamp.from(attr.lastModifiedTime().toInstant()));
        } catch (IOException ignored) {
        }
        try {
            tFile.setMimeType(Files.probeContentType(file.toPath()));
        } catch (IOException ignore) {}
        return tFile;
    }

    public TFile copy(Directory newParent, String newPath){
        TFile fileSystemItem = new TFile();
        fileSystemItem.setDiscriminator(getDiscriminator());
        fileSystemItem.setName(getName());
        fileSystemItem.setPath(newPath);
        fileSystemItem.setSize(getSize());
        fileSystemItem.setCreatedAt(getCreatedAt());
        fileSystemItem.setModifiedAt(getModifiedAt());
        fileSystemItem.setParent(newParent);
        fileSystemItem.setMimeType(getMimeType());
        return fileSystemItem;
    }

    private String getRelativePath(@Nullable Directory parent, String path){
        if(parent == null) return path;
        path = parent.getName() + "/" + path;
        if (parent.getParent() != null)
            path = getRelativePath(parent.getParent(), path);
        return path;
    }

    public FileSuggestion toSuggestion(){
        return new FileSuggestion(getFileSystemItemId(), getName(), getType(), getRelativePath(getParent(), ""));
    }

    @Data
    public static class FileSuggestion {
        @NonNull
        private Long id;
        @NonNull
        private String name;
        @NonNull
        private FileType type;
        @NonNull
        private String path;

        public InlineKeyboardButton toTelegramButton(){
            return InlineKeyboardButton.builder()
                    .text(getName())
                    .callbackData(CallbackData.create("get_file", getId().toString()))
                    .build();
        }
    }

    public WorkLogTargetFile toWorkLogTargetFile(){
        final WorkLogTargetFile file = new WorkLogTargetFile();
        file.setName(getName());
        file.setPath(getPath());
        file.setSize(getSize());
        file.setMimeType(getMimeType());
        file.setType(getType());
        file.setCreatedAt(getCreatedAt());
        return file;
    }
}
