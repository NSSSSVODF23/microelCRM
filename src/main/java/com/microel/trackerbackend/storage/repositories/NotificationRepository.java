package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Long countByEmployeeAndUnread(Employee employee, boolean unread);
    Page<Notification> findByEmployeeAndUnreadIsTrue(Employee employee, Pageable pageable);
    Page<Notification> findByEmployee(Employee employee, Pageable pageable);
    Set<Notification> findByEmployeeAndUnreadIsTrue(Employee employee);
}
