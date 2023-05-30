package com.microel.trackerbackend.services.filemanager;

import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
import io.metaloom.video4j.Video;
import io.metaloom.video4j.Video4j;
import io.metaloom.video4j.VideoFile;
import io.metaloom.video4j.Videos;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.imgscalr.Scalr;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileSaver {

    static final String ROOT_PATH = "attachments";

    public static AttachmentType getAttachmentType(FileData fileData) {
        String[] splitType = fileData.getType().split("\\/");
        switch (splitType[0]) {
            case "image":
                return AttachmentType.PHOTO;
            case "video":
                return AttachmentType.VIDEO;
            case "audio":
                return AttachmentType.AUDIO;
            case "text":
                return AttachmentType.DOCUMENT;
            case "application":
                if (Objects.equals(splitType[1], "pdf")) {
                    return AttachmentType.DOCUMENT;
                }
                return AttachmentType.FILE;
            default:
                return AttachmentType.FILE;
        }
    }

    public static String getFilesRoot(FileData fileData) {
        switch (getAttachmentType(fileData)) {
            case PHOTO:
                return "photos";
            case AUDIO:
                return "audios";
            case VIDEO:
                return "videos";
            case DOCUMENT:
                return "documents";
            case FILE:
            default:
                return "files";
        }
    }

    public static AttachmentType getTypeFromRoot(String root) {
        switch (root) {
            case "photos":
                return AttachmentType.PHOTO;
            case "audios":
                return AttachmentType.AUDIO;
            case "videos":
                return AttachmentType.VIDEO;
            case "documents":
                return AttachmentType.DOCUMENT;
            case "files":
            default:
                return AttachmentType.FILE;
        }
    }

    public ComplexPath save(FileData fileData) throws EmptyFile, WriteError {
        if (fileData.getData().length == 0)
            throw new EmptyFile();

        Path directoryPath = Path.of("./" + ROOT_PATH, getFilesRoot(fileData));
        Path thumbnailsPath = Path.of("./" + ROOT_PATH,"thumbnails");

        try {
            Files.createDirectories(directoryPath);

            Path filePath = directoryPath.resolve(fileData.getName());
            Path thumbnailPath = thumbnailsPath;

            Files.write(filePath, fileData.getData());

            AttachmentType attachmentType = getAttachmentType(fileData);

            if (attachmentType == AttachmentType.PHOTO) {
                Files.createDirectories(thumbnailsPath);
                BufferedImage bufferedImage = ImageIO.read(filePath.toFile());
                BufferedImage newImage = new BufferedImage( bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                newImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.white, null);
                BufferedImage resized = Scalr.resize(newImage, 250);
                thumbnailPath = thumbnailsPath.resolve(UUID.randomUUID() + ".jpg");
                ImageIO.write(resized, "jpg", thumbnailPath.toFile());
            } else if (attachmentType == AttachmentType.VIDEO) {
                try(VideoFile videoFile = Videos.open(filePath.toString())){
                    Files.createDirectories(thumbnailsPath);
                    BufferedImage bufferedImage = videoFile.frameToImage();
                    BufferedImage resized = Scalr.resize(bufferedImage, 250);
                    thumbnailPath = thumbnailsPath.resolve(UUID.randomUUID() + ".jpg");
                    ImageIO.write(resized, "jpg", thumbnailPath.toFile());
                }catch (Throwable ignored){
                }
            }
            return new ComplexPath(filePath.toString(), thumbnailPath.toString());
        } catch (IOException e) {
            throw new WriteError();
        }
    }

    @AllArgsConstructor
    @Getter
    public static class ComplexPath {
        private String original;
        @Nullable
        private String thumbnail;
    }
}
