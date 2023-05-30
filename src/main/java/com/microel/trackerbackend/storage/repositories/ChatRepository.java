package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.team.Employee;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long>, JpaSpecificationExecutor<Chat> {
    List<Chat> findAllByMembersContainingAndClosedIsNull(Employee employee, Sort sort);
}
