package com.microel.trackerbackend.storage.entities.filesys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.controllers.telegram.CallbackData;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import com.microel.trackerbackend.storage.entities.task.WorkLogTargetFile;
import io.metaloom.video4j.VideoFile;
import io.metaloom.video4j.Videos;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.imgscalr.Scalr;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.imageio.ImageIO;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "file")
public class TFile extends FileSystemItem {
    @Nullable
    private String mimeType;
    @Nullable
    private String thumbnail;

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

    public static TFile of(File file, Boolean createThumbnail) {
        TFile tFile = new TFile();
        tFile.setName(file.getName());
        tFile.setPath(file.getPath());
        tFile.setSize(file.length());
        Path thumbnailsPath = Path.of("./file_thumbnails");

        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            tFile.setCreatedAt(Timestamp.from(attr.creationTime().toInstant()));
            tFile.setModifiedAt(Timestamp.from(attr.lastModifiedTime().toInstant()));
        } catch (IOException ignored) {
        }
        try {
            tFile.setMimeType(Files.probeContentType(file.toPath()));
            if(createThumbnail) {
                Path thumbnailPath = null;
                if (tFile.getType() == FileType.PHOTO) {
                    try {
                        Files.createDirectories(thumbnailsPath);
                        BufferedImage bufferedImage = ImageIO.read(file);
                        BufferedImage newImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                        newImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.white, null);
                        BufferedImage resized = Scalr.resize(newImage, 250);
                        thumbnailPath = thumbnailsPath.resolve(UUID.randomUUID() + ".jpg");
                        ImageIO.write(resized, "jpg", thumbnailPath.toFile());
                    }catch (Throwable ignored) {
                        System.out.println("Не удалось создать миниатюру фото "+file);
                    }
                } else if (tFile.getType() == FileType.VIDEO) {
                    try (VideoFile videoFile = Videos.open(file.getPath())) {
                        Files.createDirectories(thumbnailsPath);
                        BufferedImage bufferedImage = videoFile.frameToImage();
                        BufferedImage resized = Scalr.resize(bufferedImage, 250);
                        thumbnailPath = thumbnailsPath.resolve(UUID.randomUUID() + ".jpg");
                        ImageIO.write(resized, "jpg", thumbnailPath.toFile());
                    } catch (Throwable ignored) {
                    }
                }
                if (thumbnailPath != null) {
                    tFile.setThumbnail(thumbnailPath.toString());
                }
            }
        } catch (IOException ignored) {}
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
        @Nullable
        private String thumbnail;

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
        file.setThumbnail(getThumbnail());
        file.setCreatedAt(getCreatedAt());
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TFile tFile)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getFileSystemItemId(), tFile.getFileSystemItemId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getFileSystemItemId());
    }
}
