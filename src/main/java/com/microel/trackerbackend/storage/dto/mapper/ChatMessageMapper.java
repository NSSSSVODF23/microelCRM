package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import org.springframework.lang.Nullable;

import java.util.HashSet;
import java.util.stream.Collectors;

public class ChatMessageMapper {
    @Nullable
    public static ChatMessageDto toDto(@Nullable ChatMessage chatMessage) {
        if (chatMessage == null) return null;
        return ChatMessageDto.builder()
                .attachment(AttachmentMapper.toDto(chatMessage.getAttachment()))
                .author(EmployeeMapper.toDto(chatMessage.getAuthor()))
                .chatMessageId(chatMessage.getChatMessageId())
                .deleted(chatMessage.getDeleted())
                .edited(chatMessage.getEdited())
                .sendAt(chatMessage.getSendAt())
                .readByEmployees(chatMessage.getReadByEmployees() == null? new HashSet<>() : chatMessage.getReadByEmployees().stream().map(EmployeeMapper::toDto).collect(Collectors.toSet()))
//                .parentChat(ChatMapper.toDto(chatMessage.getParentChat()))
                .mediaGroup(chatMessage.getMediaGroup())
                .text(chatMessage.getText())
                .replyTo(ChatMessageMapper.toDto(chatMessage.getReplyTo()))
                .telegramBinds(chatMessage.getTelegramBinds() != null ? chatMessage.getTelegramBinds().stream().map(TelegramMessageBindMapper::toDto).collect(Collectors.toSet()) : null)
                .build();
    }

    @Nullable
    public static ChatMessage fromDto(@Nullable ChatMessageDto chatMessage) {
        if (chatMessage == null) return null;
        return ChatMessage.builder()
                .attachment(AttachmentMapper.fromDto(chatMessage.getAttachment()))
                .author(EmployeeMapper.fromDto(chatMessage.getAuthor()))
                .chatMessageId(chatMessage.getChatMessageId())
                .deleted(chatMessage.getDeleted())
                .edited(chatMessage.getEdited())
                .sendAt(chatMessage.getSendAt())
                .readByEmployees(chatMessage.getReadByEmployees() == null? new HashSet<>() : chatMessage.getReadByEmployees().stream().map(EmployeeMapper::fromDto).collect(Collectors.toSet()))
//                .parentChat(ChatMapper.fromDto(chatMessage.getParentChat()))
                .mediaGroup(chatMessage.getMediaGroup())
                .text(chatMessage.getText())
                .replyTo(ChatMessageMapper.fromDto(chatMessage.getReplyTo()))
                .telegramBinds(chatMessage.getTelegramBinds() != null ? chatMessage.getTelegramBinds().stream().map(TelegramMessageBindMapper::fromDto).collect(Collectors.toSet()) : null)
                .build();
    }
}
