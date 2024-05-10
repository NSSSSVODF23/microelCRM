package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.notification.NotificationSettings;
import com.microel.trackerbackend.storage.entities.team.notification.NotificationType;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.NotificationRepository;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class NotificationDispatcher {
    private final NotificationRepository notificationRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompBroker;
    private final TelegramController telegramController;

    public NotificationDispatcher(NotificationRepository notificationRepository, EmployeeDispatcher employeeDispatcher, StompController stompBroker, TelegramController telegramController) {
        this.notificationRepository = notificationRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.stompBroker = stompBroker;
        this.telegramController = telegramController;
    }

    public Page<Notification> getNotifications(Employee employee, Long first, Integer size) throws EntryNotFound {
        return notificationRepository.findByEmployee(employee, new OffsetPageable(first, size, Sort.by(Sort.Direction.DESC, "created")));
    }

    @Transactional
    public void createNotification(Set<Employee> recipient, Notification.Factory factory) {
        // Получаем список существующих сотрудников
        Set<Employee> validEmployees = employeeDispatcher.getValidEmployees(recipient.stream().map(Employee::getLogin).collect(Collectors.toList()));
        for (Employee employee : validEmployees) {
            Notification notification = notificationRepository.save(factory.getInstance(employee));
            telegramController.sendNotification(employee, notification);
            stompBroker.sendNotification(notification);
        }
    }

    public Long getUnreadCount(String login) throws EntryNotFound {
        // Получаем сотрудника по логину
        Employee employee = employeeDispatcher.getEmployee(login);
        return notificationRepository.countByEmployeeAndUnread(employee, true);
    }

    @Transactional
    public void markAsRead(Long notificationId) throws EntryNotFound {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(EntryNotFound::new);
        notification.setUnread(false);
        notification.setWhenRead(Timestamp.from(Instant.now()));
        notificationRepository.save(notification);
    }

    @Transactional
    public void setAllAsRead(String login) throws EntryNotFound {
        // Получаем сотрудника по логину
        Employee employee = employeeDispatcher.getEmployee(login);
        // Получаем все не прочитанные уведомления
        Set<Notification> notifications = notificationRepository.findByEmployeeAndUnreadIsTrue(employee).stream().peek(notification -> {
            notification.setUnread(false);
            notification.setWhenRead(Timestamp.from(Instant.now()));
        }).collect(Collectors.toSet());
        // Сохраняем их
        notificationRepository.saveAll(notifications);
    }

    @Data
    public static class NotificationRequest {
        private Long first;
        private Integer size;
        private List<NotificationType> types;
    }
}
