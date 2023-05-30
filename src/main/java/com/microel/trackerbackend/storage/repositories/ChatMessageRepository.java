package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.team.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, JpaSpecificationExecutor<ChatMessage> {
    Page<ChatMessage> findByParentChat_ChatIdAndDeletedIsNull(Long chatId, Pageable pageable);

    Long countByParentChat_ChatIdAndReadByEmployeesNotContainsAndDeletedIsNull(Long chatId, Employee employee);

    List<ChatMessage> findAllByParentChat_ChatIdAndDeletedIsNull(Long chatId);

    Optional<ChatMessage> findFirstByTelegramBinds_TelegramChatIdAndTelegramBinds_TelegramMessageIdAndTelegramBinds_TelegramMediaGroupId(Long chatId, Integer messageId, String mediaGroupId);
}
