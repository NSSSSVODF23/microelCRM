package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.mapper.WorkLogMapper;
import com.microel.trackerbackend.storage.dto.task.WorkLogDto;
import com.microel.trackerbackend.storage.dto.task.WorkReportDto;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.AcceptingEntry;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.AlreadyClosed;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.WorkLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Transactional
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

//        HashSet<Employee> chatMembers = new HashSet<>(employees);
//        chatMembers.add(creator);
        Chat chat = Chat.builder()
                .creator(creator)
                .title("Чат из задачи #"+task.getTaskId())
                .created(Timestamp.from(Instant.now()))
                .members(Stream.of(creator).collect(Collectors.toSet()))
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


    public List<WorkLogDto> getQueueByTelegramId(Long chatId) {
        return workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<String,Employee> employeeJoin = root.join("employees", JoinType.LEFT);
            predicates.add(cb.equal(employeeJoin.get("telegramUserId"), String.valueOf(chatId)));
            predicates.add(cb.isNull(root.get("closed")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().map(WorkLogMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Возвращает активный {@link WorkLog} по идентификатору чата из telegram.
     * Активным журналом задачи, считается не закрытый имеющий монтажника как исполнителя который принял эту задачу.
     * @param chatId Идентификатор чата из telegram
     * @return Сущность {@link WorkLog}
     * @throws EntryNotFound Если активного журнала задачи не найдено
     */
    public WorkLogDto getAcceptedByTelegramId(Long chatId) throws EntryNotFound {
        return workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<String, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
            predicates.add(cb.equal(employeeJoin.get("telegramUserId"), String.valueOf(chatId)));
            predicates.add(cb.isNull(root.get("closed")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().map(WorkLogMapper::toDto)
                .filter(Objects::nonNull)
                .filter(workLog -> workLog.getAcceptedEmployees() != null && workLog.getAcceptedEmployees().stream().anyMatch(e->e.getTelegramUserId().equals(chatId.toString())))
                .findFirst().orElseThrow(() -> new EntryNotFound("Нет активной задачи"));
    }

    public ChatDto getChatByWorkLogId(Long workLogId) throws EntryNotFound {
        return workLogRepository.findById(workLogId).map(WorkLogMapper::toDto).map(WorkLogDto::getChat).orElseThrow(() -> new EntryNotFound("Чат не найден"));
    }

    public WorkLogDto acceptWorkLog(Long workLogId, Long chatId) throws EntryNotFound, AlreadyClosed, IllegalFields {
        List<WorkLog> workLogs = workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<String, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
            predicates.add(cb.equal(employeeJoin.get("telegramUserId"), String.valueOf(chatId)));
            predicates.add(cb.isNull(root.get("closed")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        WorkLog workLog = workLogs.stream().filter( w -> Objects.equals(w.getWorkLogId(), workLogId)).findFirst().orElseThrow(() -> new EntryNotFound("Работы по задаче закончены, или задачи нет в базе"));
        Employee employee = employeeDispatcher.getByTelegramId(chatId).orElseThrow(() -> new EntryNotFound("Сотрудник не найден"));
        AcceptingEntry acceptingEntry = new AcceptingEntry(employee.getLogin(), chatId.toString(), Timestamp.from(Instant.now()));
        if(!workLog.getEmployees().contains(employee)) throw new IllegalFields("Эта задача вам не назначена");
        if(workLog.getAcceptedEmployees().contains(acceptingEntry)) throw new IllegalFields("Вы уже приняли эту задачу");
        if(workLogs.stream().anyMatch(w->w.getAcceptedEmployees().contains(acceptingEntry)))throw new IllegalFields("Чтобы принять задачу, нужно завершить текущую активную");
        workLog.getAcceptedEmployees().add(acceptingEntry);
        workLog.getChat().getMembers().add(employee);
        return WorkLogMapper.toDto(workLogRepository.save(workLog));
    }
}
