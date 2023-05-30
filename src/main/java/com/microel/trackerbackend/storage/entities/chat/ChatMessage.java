package com.microel.trackerbackend.storage.entities.chat;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.mapper.AttachmentMapper;
import com.microel.trackerbackend.storage.dto.mapper.ChatMessageMapper;
import com.microel.trackerbackend.storage.dto.mapper.EmployeeMapper;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.javatuples.Pair;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_messages")
public class ChatMessage implements SuperMessageGetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;
    @Column(columnDefinition = "text default ''")
    private String text;
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @Nullable
    private Attachment attachment;
    private Timestamp edited;
    private Timestamp deleted;
    @OneToOne()
    private Employee author;
    private Timestamp sendAt;
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @Nullable
    private ChatMessage replyTo;
    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH}, targetEntity = Employee.class)
    @BatchSize(size = 25)
    private Set<Employee> readByEmployees;
    private UUID mediaGroup;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    @JsonIgnore
    private Set<TelegramMessageBind> telegramBinds;

    @ManyToOne()
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private Chat parentChat;

    public static ChatMessageDto of(String text, Employee author) {
        return ChatMessageMapper.toDto(ChatMessage.builder()
                .author(author)
                .text(text)
                .sendAt(Timestamp.from(Instant.now()))
                .readByEmployees(Stream.of(author).collect(Collectors.toSet()))
                .build());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage)) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(getChatMessageId(), that.getChatMessageId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChatMessageId());
    }

    @JsonIgnore
    public boolean hasTelegramMediaGroup() {
        return getTelegramBinds() != null && getTelegramBinds().stream().allMatch(TelegramMessageBind::isGrouped);
    }

    @JsonIgnore
    public boolean isGroupMessage() {
        return getMediaGroup() != null;
    }

    @JsonIgnore
    public boolean isSoloMessage() {
        return getMediaGroup() == null;
    }

    @JsonIgnore
    public Set<Pair<Long, String>> getGroupIdentify() {
        return getTelegramBinds().stream().map(TelegramMessageBind::groupIdentify).collect(Collectors.toSet());
    }

    public void appendBind(TelegramMessageBind bind) {
        if (telegramBinds == null)
            telegramBinds = Stream.of(bind).collect(Collectors.toSet());
        else telegramBinds.add(bind);
    }

    public boolean isExist() {
        return deleted == null;
    }

    public boolean isDeleted() {
        return deleted != null;
    }

    @Override
    public SuperMessage getSuperMessage() {
        SuperMessage superMessage = SuperMessage.builder()
                .superMessageId(chatMessageId)
                .readByEmployees(readByEmployees == null ? new HashSet<>() : readByEmployees.stream().map(EmployeeMapper::toDto).collect(Collectors.toSet()))
                .text(text)
                .sendAt(sendAt)
                .edited(edited)
                .replyTo(replyTo == null ? null : replyTo.getSuperMessage())
                .deleted(isDeleted())
                .author(EmployeeMapper.toDto(author))
                .parentChatId(parentChat == null ? null :parentChat.getChatId())
                .includedMessages(Stream.of(chatMessageId).collect(Collectors.toSet()))
                .build();
        if (attachment != null) {
            superMessage.setAttachments(Stream.of(AttachmentMapper.toDto(attachment)).collect(Collectors.toList()));
        } else {
            superMessage.setAttachments(new ArrayList<>());
        }
        return superMessage;
    }
}
