package com.microel.trackerbackend.storage.dto.chat;

import lombok.*;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TelegramMessageBindDto {
    private Long telegramMessageBindId;
    private Long telegramChatId;
    private Integer telegramMessageId;
    private String telegramMediaGroupId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelegramMessageBindDto)) return false;
        TelegramMessageBindDto that = (TelegramMessageBindDto) o;
        return Objects.equals(getTelegramMessageBindId(), that.getTelegramMessageBindId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTelegramMessageBindId());
    }
}
