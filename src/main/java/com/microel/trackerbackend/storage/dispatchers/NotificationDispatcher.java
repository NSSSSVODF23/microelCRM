package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.EmployeeStatus;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Component
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

    public Page<Notification> getNotifications(String login, Long first, Integer size, Boolean unreadOnly) throws EntryNotFound {
        // Получаем сотрудника по логину
        Employee employee = employeeDispatcher.getEmployee(login);
        if(unreadOnly){
            return notificationRepository.findByEmployeeAndUnreadIsTrue(employee, new OffsetPageable(first, size, Sort.by(Sort.Direction.DESC, "created")));
        }else{
            return notificationRepository.findByEmployee(employee, new OffsetPageable(first, size, Sort.by(Sort.Direction.DESC, "created")));
        }
    }

    public void createNotification(Set<Employee> recipient, Notification.Factory factory) {
        // Получаем список существующих сотрудников
        Set<Employee> validEmployees = employeeDispatcher.getValidEmployees(recipient.stream().map(Employee::getLogin).collect(Collectors.toList()));
        for (Employee employee : validEmployees) {
            Notification savedNotification = notificationRepository.save(factory.getInstace(employee));
            if(employee.getStatus() != EmployeeStatus.ONLINE) telegramController.sendNotification(employee, savedNotification);
            stompBroker.sendNotification(savedNotification);
        }
    }

    public Long getUnreadCount(String login) throws EntryNotFound {
        // Получаем сотрудника по логину
        Employee employee = employeeDispatcher.getEmployee(login);
        return notificationRepository.countByEmployeeAndUnread(employee, true);
    }

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
}
