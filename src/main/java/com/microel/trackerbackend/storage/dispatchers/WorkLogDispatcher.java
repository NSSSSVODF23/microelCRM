package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.WorkLogRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class WorkLogDispatcher {
    private final WorkLogRepository workLogRepository;
    private final EmployeeDispatcher employeeDispatcher;

    public WorkLogDispatcher(WorkLogRepository workLogRepository, EmployeeDispatcher employeeDispatcher) {
        this.workLogRepository = workLogRepository;
        this.employeeDispatcher = employeeDispatcher;
    }

    public WorkLog createWorkLog(Task task, Set<Employee> employees, Employee creator) throws EntryNotFound, IllegalFields {
        if (task == null) throw new EntryNotFound("Не найдена задача");
        if(task.getTaskStatus() != TaskStatus.ACTIVE) throw new IllegalFields("Нельзя назначать сотрудников на не активную задачу");
        for (Employee installer : employees) {
            if(installer == null || installer.getLogin() == null || installer.getLogin().isBlank()) throw new IllegalFields("Не задан логин сотрудника");
            Employee employeeByLogin = employeeDispatcher.getEmployee(installer.getLogin());
            if (employeeByLogin == null) throw new EntryNotFound("Не найден сотрудник с логином " + installer.getLogin());
            if(employeeByLogin.getDeleted()) throw new EntryNotFound("Сотрудник с логином " + installer.getLogin() + " удален");
            if(!employeeByLogin.getOffsite()) throw new IllegalFields("Сотрудник с логином " + installer.getLogin() + " не монтажник");
        }

        HashSet<Employee> chatMembers = new HashSet<>(employees);
        chatMembers.add(creator);
        Chat chat = Chat.builder()
                .creator(creator)
                .created(Timestamp.from(Instant.now()))
                .members(chatMembers)
                .deleted(false)
                .updated(Timestamp.from(Instant.now()))
                .build();

        WorkLog workLog = WorkLog.builder()
                .chat(chat)
                .created(Timestamp.from(Instant.now()))
                .task(task)
                .isForceClosed(false)
                .employees(employees)
                .creator(creator)
                .build();

        return workLogRepository.save(workLog);
    }

    public Optional<WorkLog> getActiveWorkLogByTask(Task task) {
        return workLogRepository.findAllByTaskAndClosedIsNull(task);
    }

    public WorkLog getActiveWorkLogByTaskId(Long taskId) throws EntryNotFound {
        return workLogRepository.findAllByTask_TaskIdAndClosedIsNull(taskId).orElseThrow(()->new EntryNotFound("WorkLog не найден"));
    }

    public WorkLog get(Long id) throws EntryNotFound {
        return workLogRepository.findById(id).orElseThrow(() -> new EntryNotFound("Отчет не найден"));
    }

    public WorkLog save(WorkLog workLog) {
        return workLogRepository.save(workLog);
    }
}
