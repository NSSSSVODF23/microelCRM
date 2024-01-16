package com.microel.trackerbackend.services.filemanager;

import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import io.metaloom.video4j.VideoFile;
import io.metaloom.video4j.Videos;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.imgscalr.Scalr;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class FileSaver {

    private final String ROOT_PATH;

    public FileSaver(){
        ROOT_PATH = "attachments";
    }
    public FileSaver(String rootPath) {
        ROOT_PATH = rootPath;
    }

    public static FileType getFileType(FileData fileData) {
        String[] splitType = fileData.getType().split("\\/");
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

    public static String getFilesRoot(FileData fileData) {
        switch (getFileType(fileData)) {
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

    public static FileType getTypeFromRoot(String root) {
        switch (root) {
            case "photos":
                return FileType.PHOTO;
            case "audios":
                return FileType.AUDIO;
            case "videos":
                return FileType.VIDEO;
            case "documents":
                return FileType.DOCUMENT;
            case "files":
            default:
                return FileType.FILE;
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

            FileType fileType = getFileType(fileData);

            if (fileType == FileType.PHOTO) {
                Files.createDirectories(thumbnailsPath);
                BufferedImage bufferedImage = ImageIO.read(filePath.toFile());
                BufferedImage newImage = new BufferedImage( bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                newImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.white, null);
                BufferedImage resized = Scalr.resize(newImage, 250);
                thumbnailPath = thumbnailsPath.resolve(UUID.randomUUID() + ".jpg");
                ImageIO.write(resized, "jpg", thumbnailPath.toFile());
            } else if (fileType == FileType.VIDEO) {
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
