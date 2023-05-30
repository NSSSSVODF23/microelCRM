package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import org.springframework.lang.Nullable;

import java.util.stream.Collectors;

public class ChatMapper {
    @Nullable
    public static ChatDto toDto(@Nullable Chat chat){
        if(chat == null) return null;
        return ChatDto.builder()
                .chatId(chat.getChatId())
                .title(chat.getTitle())
                .created(chat.getCreated())
                .creator(EmployeeMapper.toDto(chat.getCreator()))
                .deleted(chat.getDeleted())
                .lastMessage(ChatMessageMapper.toDto(chat.getLastMessage()))
                .members(chat.getMembers().stream().map(EmployeeMapper::toDto).collect(Collectors.toSet()))
//                .messages(chat.getMessages().stream().map(ChatMessageMapper::toDto).collect(Collectors.toSet()))
                .updated(chat.getUpdated())
                .closed(chat.getClosed())
                .build();
    }

    @Nullable
    public static Chat fromDto(@Nullable ChatDto chat){
        if(chat == null) return null;
        return Chat.builder()
                .chatId(chat.getChatId())
                .title(chat.getTitle())
                .created(chat.getCreated())
                .creator(EmployeeMapper.fromDto(chat.getCreator()))
                .deleted(chat.getDeleted())
                .lastMessage(ChatMessageMapper.fromDto(chat.getLastMessage()))
                .members(chat.getMembers().stream().map(EmployeeMapper::fromDto).collect(Collectors.toSet()))
//                .messages(chat.getMessages().stream().map(ChatMessageMapper::fromDto).collect(Collectors.toSet()))
                .updated(chat.getUpdated())
                .closed(chat.getClosed())
                .build();
    }
}
