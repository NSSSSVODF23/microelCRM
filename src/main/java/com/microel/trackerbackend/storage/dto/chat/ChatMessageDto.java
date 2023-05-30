package com.microel.trackerbackend.storage.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.comment.AttachmentDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.chat.TelegramMessageBind;
import lombok.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ChatMessageDto {
    private Long chatMessageId;
    private AttachmentDto attachment;
    private EmployeeDto author;
    private Timestamp deleted;
    private Timestamp edited;
    private Timestamp sendAt;
    private ChatMessageDto replyTo;
    private Set<EmployeeDto> readByEmployees;
    private UUID mediaGroup;
    private ChatDto parentChat;
    private String text;
    private Set<TelegramMessageBindDto> telegramBinds;
}
