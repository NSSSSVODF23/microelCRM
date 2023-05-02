package com.microel.trackerbackend.storage.entities.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;
    @OneToMany(mappedBy = "parentChat", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @BatchSize(size = 25)
    private Set<ChatMessage> messages;
    private Boolean deleted;
    private Timestamp created;
    @OneToOne
    @JoinColumn(name = "f_creator_login")
    private Employee creator;
    @ManyToMany()
    @BatchSize(size = 25)
    private Set<Employee> members;
    private Timestamp updated;
    @OneToOne()
    @JsonManagedReference
    @JoinColumn(name = "f_last_message_id")
    private ChatMessage lastMessage;

    public void addMessage(ChatMessage message) {
        message.setParentChat(this);
        messages.add(message);
    }
}
