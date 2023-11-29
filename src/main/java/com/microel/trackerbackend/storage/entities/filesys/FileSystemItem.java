package com.microel.trackerbackend.storage.entities.filesys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.api.ResponseException;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.io.File;
import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "file_system_item")
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(value = "item")
public class FileSystemItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileSystemItemId;
    @Column(insertable = false, updatable = false)
    private String discriminator;
    private String name;
    @JsonIgnore
    private String path;
    private Long size;
    private Timestamp createdAt;
    private Timestamp modifiedAt;
    @ManyToOne
    @JoinColumn(name = "f_parent_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Nullable
    private Directory parent;

    public FileSystemItem copy(Directory newParent, String newPath){
        if(this instanceof Directory) {
            return ((Directory) this).copy(newParent, newPath);
        }else if(this instanceof TFile){
            return ((TFile) this).copy(newParent, newPath);
        }
        throw new ResponseException("Неизвестный класс объекта копирования");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileSystemItem that)) return false;
        return Objects.equals(getFileSystemItemId(), that.getFileSystemItemId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileSystemItemId());
    }
}
