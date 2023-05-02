package com.microel.trackerbackend.storage.entities.chat;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;
    @Column(columnDefinition = "text default ''")
    private String text;
    @OneToMany()
    @BatchSize(size = 25)
    private Set<Attachment> attachments;
    private Timestamp edited;
    private Timestamp deleted;
    @OneToOne()
    private Employee author;
    private Timestamp sendAt;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private Chat parentChat;

    public static ChatMessage of(MessageData messageData, Employee author){
        return ChatMessage.builder()
//                .attachments(message.getAttachments())
                .author(author)
                .text(messageData.getText())
                .sendAt(Timestamp.from(Instant.now()))
                .build();
    }
}
