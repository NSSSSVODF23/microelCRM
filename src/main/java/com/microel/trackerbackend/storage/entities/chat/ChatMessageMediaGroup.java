package com.microel.trackerbackend.storage.entities.chat;

import com.microel.trackerbackend.storage.dto.mapper.AttachmentMapper;
import com.microel.trackerbackend.storage.dto.mapper.EmployeeMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageMediaGroup implements SuperMessageGetter {
    private List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
    }

    @Override
    public SuperMessage getSuperMessage() {
        if(messages.isEmpty()) return null;
        messages.sort(Comparator.comparing(ChatMessage::getChatMessageId));
        return SuperMessage.builder()
                .superMessageId(messages.get(0).getChatMessageId())
                .attachments(messages.stream()
                        .filter(ChatMessage::isExist)
                        .map(ChatMessage::getAttachment)
                        .filter(Objects::nonNull)
                        .map(AttachmentMapper::toDto)
                        .collect(Collectors.toList())
                )
                .readByEmployees(messages.get(0).getReadByEmployees() == null ? new HashSet<>() : messages.get(0).getReadByEmployees().stream().map(EmployeeMapper::toDto).collect(Collectors.toSet()))
                .text(messages.get(0).getText())
                .replyTo(messages.get(0).getReplyTo() == null ? null : messages.get(0).getReplyTo().getSuperMessage())
                .sendAt(messages.get(0).getSendAt())
                .edited(messages.get(0).getEdited())
                .deleted(messages.stream().allMatch(ChatMessage::isDeleted))
                .author(EmployeeMapper.toDto(messages.get(0).getAuthor()))
                .includedMessages(messages.stream().map(ChatMessage::getChatMessageId).collect(Collectors.toSet()))
                .parentChatId(messages.get(0).getParentChat().getChatId())
                .build();
    }
}
