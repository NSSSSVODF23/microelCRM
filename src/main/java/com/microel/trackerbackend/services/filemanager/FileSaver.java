package com.microel.trackerbackend.services.filemanager;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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

    public String save(FileData fileData) throws EmptyFile, WriteError {
        if (fileData.getData().length == 0)
            throw new EmptyFile();

        Path filePatch = Path.of("./"+ROOT_PATH, getFilesRoot(fileData));

        try {
            Files.createDirectories(filePatch);
            Path filePath = Path.of(String.valueOf(filePatch), fileData.getName());
            Files.write(filePath, fileData.getData());
            return String.valueOf(filePath);
        } catch (IOException e) {
            throw new WriteError();
        }
    }
}
