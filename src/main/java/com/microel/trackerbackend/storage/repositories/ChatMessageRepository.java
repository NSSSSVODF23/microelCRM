package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, JpaSpecificationExecutor<ChatMessage> {
    Page<ChatMessage> findByParentChat_ChatId(Long chatId, Pageable pageable);
}
