package com.microel.trackerbackend.storage.entities.comments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

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
    private AttachmentType type;
    @Column(length = 4096)
    @JsonIgnore
    private String path;
    private Long size;
    private Timestamp created;
    private Timestamp modified;
    @ManyToMany(mappedBy = "attachments")
    @JsonIgnore
    @BatchSize(size = 25)
    private List<Comment> comments;
}
