package com.microel.trackerbackend.storage.entities.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.javatuples.Pair;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "telegram_message_binds")
public class TelegramMessageBind {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long telegramMessageBindId;
    private Long telegramChatId;
    private Integer telegramMessageId;
    @Column(columnDefinition = "varchar(64) default null")
    private String telegramMediaGroupId;

    public static TelegramMessageBind from(Message message) {
        return TelegramMessageBind.builder()
                .telegramChatId(message.getChatId())
                .telegramMessageId(message.getMessageId())
                .telegramMediaGroupId(message.getMediaGroupId())
                .build();
    }

    @JsonIgnore
    public Pair<Long, String> groupIdentify(){
        return Pair.with(telegramChatId, telegramMediaGroupId);
    }

    @JsonIgnore
    public boolean isGrouped(){
        return telegramChatId != null && telegramMediaGroupId != null;
    }

    @JsonIgnore
    public boolean isSolo() {
        return telegramChatId != null && telegramMediaGroupId == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelegramMessageBind that)) return false;
        return Objects.equals(getTelegramChatId(), that.getTelegramChatId()) && Objects.equals(getTelegramMediaGroupId(), that.getTelegramMediaGroupId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTelegramChatId(), getTelegramMediaGroupId());
    }
}
