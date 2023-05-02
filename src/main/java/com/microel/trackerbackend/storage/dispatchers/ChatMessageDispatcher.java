package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.repositories.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageDispatcher {
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageDispatcher(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public Page<ChatMessage> getChatMessages(Long chatId, Long first, Integer limit) {
        return chatMessageRepository.findByParentChat_ChatId(chatId,
                new OffsetPageable(first, limit, Sort.by(Sort.Direction.DESC, "sendAt")));
    }
}
