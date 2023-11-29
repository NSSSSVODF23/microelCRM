package com.microel.trackerbackend.storage.entities.filesys;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "directory")
public class Directory extends FileSystemItem {
    private Integer fileCount;

    public static Directory of(File file) {
        Directory directory = new Directory();
        directory.setName(file.getName());
        directory.setPath(file.getPath());
        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            directory.setCreatedAt(Timestamp.from(attr.creationTime().toInstant()));
            directory.setModifiedAt(Timestamp.from(attr.lastModifiedTime().toInstant()));
        } catch (IOException ignored) {
        }
        File[] files = file.listFiles();
        if(files != null)
            directory.setFileCount(files.length);
        return directory;
    }

    public Directory copy(Directory newParent, String newPath){
        Directory fileSystemItem = new Directory();
        fileSystemItem.setDiscriminator(getDiscriminator());
        fileSystemItem.setName(getName());
        fileSystemItem.setPath(newPath);
        fileSystemItem.setSize(getSize());
        fileSystemItem.setCreatedAt(getCreatedAt());
        fileSystemItem.setModifiedAt(getModifiedAt());
        fileSystemItem.setParent(newParent);
        fileSystemItem.setFileCount(getFileCount());
        return fileSystemItem;
    }
}
