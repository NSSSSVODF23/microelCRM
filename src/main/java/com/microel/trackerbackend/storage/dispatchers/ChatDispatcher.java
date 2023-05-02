package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.ChatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Component
public class ChatDispatcher {
    private final ChatRepository chatRepository;
    private final ChatMessageDispatcher chatMessageDispatcher;

    public ChatDispatcher(ChatRepository chatRepository, ChatMessageDispatcher chatMessageDispatcher) {
        this.chatRepository = chatRepository;
        this.chatMessageDispatcher = chatMessageDispatcher;
    }

    public Pair<Chat,ChatMessage> sendToChat(Long chatId, ChatMessage message) throws EntryNotFound {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new EntryNotFound("Чат не найден"));
        chat.addMessage(message);
        chatRepository.save(chat);
        return Pair.of(chat, message);
    }

    public Page<ChatMessage> getChatMessages(Long chatId, Long first, Integer limit) throws EntryNotFound {
        chatRepository.findById(chatId).orElseThrow(() -> new EntryNotFound("Чат не найден"));
        return chatMessageDispatcher.getChatMessages(chatId, first, limit);
    }
}
