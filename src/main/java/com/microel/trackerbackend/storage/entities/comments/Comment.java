package com.microel.trackerbackend.storage.entities.comments;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.microel.trackerbackend.storage.entities.comments.dto.CommentDto;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comments")
public class Comment implements TaskJournalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;
    @Column(columnDefinition = "text default ''")
    private String message;
    private Timestamp created;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_employee_id")
    private Employee creator;
    @ManyToMany(cascade = CascadeType.PERSIST)
    @BatchSize(size = 25)
    private List<Attachment> attachments;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_reply_comment_id")
    private Comment replyComment;
    private Boolean edited;
    private Boolean deleted;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JsonBackReference
    private Task parent;

    public CommentDto toDto() {
        return new CommentDto() {
            @Override
            public Long getCommentId() {
                return commentId;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public Timestamp getCreated() {
                return created;
            }

            @Override
            public Employee getCreator() {
                return creator;
            }

            @Override
            public List<Attachment> getAttachments() {
                return attachments;
            }

            @Override
            public CommentDto getReplyComment() {
                return replyComment != null ? replyComment.toDto() : null;
            }

            @Override
            public Boolean getEdited() {
                return edited;
            }

            @Override
            public Boolean getDeleted() {
                return deleted;
            }
        };
    }
}
