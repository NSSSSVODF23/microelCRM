package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.chat.TelegramMessageBindDto;
import com.microel.trackerbackend.storage.entities.chat.TelegramMessageBind;
import org.springframework.lang.Nullable;

public class TelegramMessageBindMapper {
    @Nullable
    public static TelegramMessageBindDto toDto(@Nullable TelegramMessageBind telegramMessageBind) {
        if (telegramMessageBind == null) {
            return null;
        }
        return TelegramMessageBindDto.builder()
                .telegramMessageBindId(telegramMessageBind.getTelegramMessageBindId())
                .telegramChatId(telegramMessageBind.getTelegramChatId())
                .telegramMessageId(telegramMessageBind.getTelegramMessageId())
                .telegramMediaGroupId(telegramMessageBind.getTelegramMediaGroupId())
                .build();
    }

    @Nullable
    public static TelegramMessageBind fromDto(@Nullable TelegramMessageBindDto telegramMessageBind) {
        if (telegramMessageBind == null) {
            return null;
        }
        return TelegramMessageBind.builder()
                .telegramMessageBindId(telegramMessageBind.getTelegramMessageBindId())
                .telegramChatId(telegramMessageBind.getTelegramChatId())
                .telegramMessageId(telegramMessageBind.getTelegramMessageId())
                .telegramMediaGroupId(telegramMessageBind.getTelegramMediaGroupId())
                .build();
    }
}
