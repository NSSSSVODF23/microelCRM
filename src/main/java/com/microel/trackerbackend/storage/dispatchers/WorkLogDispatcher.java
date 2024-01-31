package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.BypassWorkCalculationForm;
import com.microel.trackerbackend.services.FilesWatchService;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerService;
import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.mapper.WorkLogMapper;
import com.microel.trackerbackend.storage.dto.task.WorkLogDto;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.entities.task.*;
import com.microel.trackerbackend.storage.entities.task.utils.AcceptingEntry;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.exceptions.AlreadyClosed;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.WorkLogRepository;
import com.microel.trackerbackend.storage.repositories.WorkLogTargetFileRepository;
import com.microel.trackerbackend.storage.repositories.WorkReportRepository;
import lombok.Data;
import org.hibernate.Hibernate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Transactional(readOnly = true)
public class WorkLogDispatcher {
    private final WorkLogRepository workLogRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final TaskEventDispatcher taskEventDispatcher;
    private final TaskDispatcher taskDispatcher;
    private final StompController stompController;
    private final NotificationDispatcher notificationDispatcher;
    private final WorkReportRepository workReportRepository;
    private final WorkLogTargetFileRepository workLogTargetFileRepository;
    private final OldTrackerService oldTrackerService;
    private final FilesWatchService filesWatchService;

    public WorkLogDispatcher(WorkLogRepository workLogRepository, EmployeeDispatcher employeeDispatcher, TaskEventDispatcher taskEventDispatcher, @Lazy TaskDispatcher taskDispatcher, StompController stompController, @Lazy NotificationDispatcher notificationDispatcher,
                             WorkReportRepository workReportRepository, WorkLogTargetFileRepository workLogTargetFileRepository, OldTrackerService oldTrackerService, FilesWatchService filesWatchService) {
        this.workLogRepository = workLogRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.taskEventDispatcher = taskEventDispatcher;
        this.taskDispatcher = taskDispatcher;
        this.stompController = stompController;
        this.notificationDispatcher = notificationDispatcher;
        this.workReportRepository = workReportRepository;
        this.workLogTargetFileRepository = workLogTargetFileRepository;
        this.oldTrackerService = oldTrackerService;
        this.filesWatchService = filesWatchService;
    }

    @Transactional
    public WorkLog createWorkLog(Task task, WorkLog.AssignBody assignBody, Employee creator) throws EntryNotFound, IllegalFields {
        if (task == null) throw new EntryNotFound("Не найдена задача");
        if (task.getTaskStatus() != TaskStatus.ACTIVE)
            throw new IllegalFields("Нельзя назначать сотрудников на не активную задачу");
        for (Employee installer : assignBody.getInstallers()) {
            if (installer == null || installer.getLogin() == null || installer.getLogin().isBlank())
                throw new IllegalFields("Не задан логин сотрудника");
            Employee employeeByLogin = employeeDispatcher.getEmployee(installer.getLogin());
            if (employeeByLogin == null)
                throw new EntryNotFound("Не найден сотрудник с логином " + installer.getLogin());
            if (employeeByLogin.getDeleted())
                throw new EntryNotFound("Сотрудник с логином " + installer.getLogin() + " удален");
            if (!employeeByLogin.getOffsite())
                throw new IllegalFields("Сотрудник с логином " + installer.getLogin() + " не монтажник");
        }

//        HashSet<Employee> chatMembers = new HashSet<>(employees);
//        chatMembers.add(creator);
        Chat chat = Chat.builder()
                .creator(creator)
                .title("Чат из задачи #" + task.getTaskId())
                .created(Timestamp.from(Instant.now()))
                .members(
                        Stream.of(creator)
                                .collect(Collectors.toSet())
                )
                .deleted(false)
                .updated(Timestamp.from(Instant.now()))
                .build();

        WorkLog workLog = WorkLog.builder()
                .chat(chat)
                .created(Timestamp.from(Instant.now()))
                .task(task)
                .isForceClosed(false)
                .targetDescription(assignBody.getDescription())
                .employees(assignBody.getInstallers())
                .gangLeader(assignBody.getGangLeader())
                .deferredReport(assignBody.getDeferredReport())
                .creator(creator)
                .workReports(new HashSet<>())
                .acceptedEmployees(new HashSet<>())
                .calculated(false)
                .build();


        List<WorkLogTargetFile> targetFiles = new ArrayList<>();
        if (assignBody.getFiles() != null && !assignBody.getFiles().isEmpty()) {
            for (FileData fileData : assignBody.getFiles()) {
                try {
                    targetFiles.add(WorkLogTargetFile.of(fileData));
                } catch (EmptyFile e) {
                    throw new ResponseException("Невозможно сохранить пустой файл");
                } catch (WriteError e) {
                    throw new ResponseException("Ошибка сохранения файла цели работы");
                }
            }
        }
        if (assignBody.getServerFiles() != null && !assignBody.getServerFiles().isEmpty()) {
            for (TFile.FileSuggestion fileSuggestion : assignBody.getServerFiles()) {
                filesWatchService.getFileById(fileSuggestion.getId()).ifPresent(file -> {
                    targetFiles.add(file.toWorkLogTargetFile());
                });
            }
        }

        workLog.setTargetFiles(targetFiles);
        return workLogRepository.save(workLog);
    }

    @Transactional
    public WorkLog createWorkLog(Task task, BypassWorkCalculationForm.InstallersReportForm installersReportForm, Timestamp creatingDate, Employee creator) throws EntryNotFound, IllegalFields {
        if (task == null) throw new EntryNotFound("Не найдена задача");
        if (task.getTaskStatus() != TaskStatus.ACTIVE)
            throw new IllegalFields("Нельзя назначать сотрудников на не активную задачу");
        for (Employee installer : installersReportForm.getInstallers()) {
            if (installer == null || installer.getLogin() == null || installer.getLogin().isBlank())
                throw new IllegalFields("Не задан логин сотрудника");
            Employee employeeByLogin = employeeDispatcher.getEmployee(installer.getLogin());
            if (employeeByLogin == null)
                throw new EntryNotFound("Не найден сотрудник с логином " + installer.getLogin());
            if (employeeByLogin.getDeleted())
                throw new EntryNotFound("Сотрудник с логином " + installer.getLogin() + " удален");
            if (!employeeByLogin.getOffsite())
                throw new IllegalFields("Сотрудник с логином " + installer.getLogin() + " не монтажник");
        }

        Chat chat = Chat.builder().creator(creator).title("Чат из задачи #" + task.getTaskId()).created(creatingDate).members(Stream.of(creator).collect(Collectors.toSet())).deleted(false).updated(creatingDate).build();

        WorkLog workLog = WorkLog.builder()
                .chat(chat)
                .created(creatingDate)
                .task(task)
                .isForceClosed(false)
                .employees(installersReportForm.getInstallers())
                .creator(creator)
                .closed(creatingDate)
                .acceptedEmployees(
                        installersReportForm.getInstallers()
                                .stream().map((e) -> AcceptingEntry.of(e, creatingDate))
                                .collect(Collectors.toSet())
                ).calculated(false).build();

        workLog.setWorkReports(
                installersReportForm.getInstallers()
                        .stream().map((e) -> {
                            return WorkReport.builder()
                                    .author(e)
                                    .created(creatingDate)
                                    .description(installersReportForm.getReport())
                                    .workLog(workLog)
                                    .awaitingWriting(false)
                                    .build();
                        }).collect(Collectors.toSet()));

        return workLogRepository.save(workLog);
    }

    public Optional<WorkLog> getActiveWorkLogByTask(Task task) {
        return workLogRepository.findAllByTaskAndClosedIsNull(task);
    }

    public WorkLog getActiveWorkLogByTaskId(Long taskId) throws EntryNotFound {
        return workLogRepository.findAllByTask_TaskIdAndClosedIsNull(taskId).orElseThrow(() -> new EntryNotFound("WorkLog не найден"));
    }

    public WorkLog get(Long id) throws EntryNotFound {
        return workLogRepository.findById(id).orElseThrow(() -> new EntryNotFound("Отчет не найден"));
    }

    @Transactional
    public WorkLog save(WorkLog workLog) {
        return workLogRepository.save(workLog);
    }


    public List<WorkLog> getQueueByTelegramId(Long chatId) {
        List<WorkLog> workLogs = workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<WorkReport> subquery = query.subquery(WorkReport.class);
            Root<WorkReport> workReportRoot = subquery.from(WorkReport.class);
            subquery.select(workReportRoot).distinct(true).where(cb.equal(workReportRoot.join("author").get("telegramUserId"), String.valueOf(chatId)));

            Join<String, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
            predicates.add(cb.equal(employeeJoin.get("telegramUserId"), String.valueOf(chatId)));

            Join<Object, Object> workReportsJoin = root.join("workReports", JoinType.LEFT);
            predicates.add(cb.or(workReportsJoin.isNull(), cb.in(workReportsJoin).value(subquery).not()));

            predicates.add(cb.isNull(root.get("closed")));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        workLogs.forEach(workLog -> Hibernate.initialize(workLog.getTask().getFields()));
        return workLogs;
    }

    public List<WorkLog> getQueueWorkLogByEmployee(Employee employee) {
        List<WorkLog> workLogs = workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<WorkReport> workReportSubquery = query.subquery(WorkReport.class);
            Root<WorkReport> workReportRoot = workReportSubquery.from(WorkReport.class);
            workReportSubquery.select(workReportRoot).distinct(true).where(cb.equal(workReportRoot.join("author"), employee));

            Join<WorkLog, WorkReport> workReportsJoin = root.join("workReports", JoinType.LEFT);
            predicates.add(cb.or(workReportsJoin.isNull(), workReportsJoin.in(workReportSubquery).not()));

            Join<WorkLog, Employee> employeesJoin = root.join("employees", JoinType.LEFT);
            predicates.add(employeesJoin.in(employee));
            predicates.add(cb.isNull(root.get("closed")));

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        if (workLogs.isEmpty()) throw new EntryNotFound("Не найдено назначенных работ");

        return workLogs;
    }

    public List<ModelItem> getTaskFieldsAcceptedWorkLog(Employee employee) {
        Hibernate.initialize(getAcceptedWorkLogByEmployee(employee).getTask().getFields());
        return getAcceptedWorkLogByEmployee(employee).getTask().getFields();
    }

    public WorkLog getAcceptedWorkLogByEmployee(Employee employee) {
        return getQueueWorkLogByEmployee(employee).stream().filter(wl -> {
            return wl.getAcceptedEmployees().stream().anyMatch(ae -> Objects.equals(ae.getLogin(), employee.getLogin()));
        }).findFirst().orElseThrow(() -> new EntryNotFound("Не найдено активной задачи"));
    }

    @Nullable
    public WorkLog getAcceptedWorkLogByTelegramId(Long chatId) {
        return workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<WorkReport> subquery = query.subquery(WorkReport.class);
            Root<WorkReport> workReportRoot = subquery.from(WorkReport.class);
            subquery.select(workReportRoot).distinct(true).where(cb.equal(workReportRoot.join("author").get("telegramUserId"), String.valueOf(chatId)));

            Join<WorkLog, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
            predicates.add(cb.equal(employeeJoin.get("telegramUserId"), String.valueOf(chatId)));

            Join<WorkLog, WorkReport> workReportsJoin = root.join("workReports", JoinType.LEFT);
            predicates.add(cb.or(workReportsJoin.isNull(), cb.in(workReportsJoin).value(subquery).not()));

            predicates.add(cb.isNull(root.get("closed")));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().findFirst().orElse(null);
    }

    /**
     * Возвращает активный {@link WorkLog} по идентификатору чата из telegram.
     * Активным журналом задачи, считается не закрытый имеющий монтажника как исполнителя который принял эту задачу.
     *
     * @param chatId Идентификатор чата из telegram
     * @return Сущность {@link WorkLog}
     * @throws EntryNotFound Если активного журнала задачи не найдено
     */
    public WorkLog getAcceptedByTelegramIdDTO(Long chatId) throws EntryNotFound {
        WorkLog foundWorkLog = getQueueByTelegramId(chatId)
                .stream()
                .filter(workLog -> workLog.getAcceptedEmployees() != null && workLog.getAcceptedEmployees().stream().anyMatch(e -> e.getTelegramUserId().equals(chatId.toString())))
                .findFirst()
                .orElseThrow(() -> new EntryNotFound("Нет активной задачи"));
        for (ModelItem modelItem : foundWorkLog.getTask().getFields()) {
            modelItem.getTextRepresentationForTlg();
        }
        return foundWorkLog;
    }

    /**
     * Возвращает активный чат задачи монтажника
     *
     * @param employee
     * @return
     */
    public Chat getActiveChatByEmployee(Employee employee) {
        Chat chat = workLogRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.isNull(root.get("closed")));
                    Join<WorkLog, WorkReport> reportJoin = root.join("workReports", JoinType.LEFT);
                    Join<WorkLog, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
                    predicates.add(employeeJoin.in(employee));
                    predicates.add(cb.or(reportJoin.isNull(), reportJoin.get("author").in(employee).not()));
                    query.distinct(true);

                    return cb.and(predicates.toArray(Predicate[]::new));
                })
                .stream()
                .filter(workLog -> workLog.getAcceptedEmployees().stream().map(AcceptingEntry::getLogin)
                        .anyMatch(login -> Objects.equals(employee.getLogin(), login)))
                .findFirst()
                .orElseThrow(() -> new ResponseException("У монтажника " + employee.getFullName() + " нет активного чата"))
                .getChat();
        Hibernate.initialize(chat.getMembers());
        return chat;
    }

    /**
     * Возвращает активный {@link WorkLog} по идентификатору чата из telegram.
     * Активным журналом задачи, считается не закрытый имеющий монтажника как исполнителя который принял эту задачу.
     *
     * @param chatId Идентификатор чата из telegram
     * @return Сущность {@link WorkLog}
     * @throws EntryNotFound Если активного журнала задачи не найдено
     */
    public WorkLog getAcceptedByTelegramId(Long chatId) throws EntryNotFound {
        WorkLog acceptedWorkLogByTelegramId = getAcceptedWorkLogByTelegramId(chatId);
        if (acceptedWorkLogByTelegramId == null) throw new EntryNotFound("Нет активной задачи");
        return acceptedWorkLogByTelegramId;
    }

    public ChatDto getChatByWorkLogId(Long workLogId) throws EntryNotFound {
        return workLogRepository.findById(workLogId).map(WorkLogMapper::toDto).map(WorkLogDto::getChat).orElseThrow(() -> new EntryNotFound("Чат не найден"));
    }

    @Transactional
    public WorkLog acceptWorkLog(Long workLogId, Long chatId) throws EntryNotFound, AlreadyClosed, IllegalFields {
        List<WorkLog> workLogs = workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<String, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
            predicates.add(cb.equal(employeeJoin.get("telegramUserId"), String.valueOf(chatId)));
            predicates.add(cb.isNull(root.get("closed")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        WorkLog workLog = workLogs.stream().filter(w -> Objects.equals(w.getWorkLogId(), workLogId)).findFirst().orElseThrow(() -> new EntryNotFound("Работы по задаче закончены, или задачи нет в базе"));
        Employee employee = employeeDispatcher.getByTelegramId(chatId).orElseThrow(() -> new EntryNotFound("Сотрудник не найден"));
        AcceptingEntry acceptingEntry = new AcceptingEntry(employee.getLogin(), chatId.toString(), Timestamp.from(Instant.now()));
        if (!workLog.getEmployees().contains(employee)) throw new IllegalFields("Эта задача вам не назначена");
        if (workLog.getAcceptedEmployees().contains(acceptingEntry))
            throw new IllegalFields("Вы уже приняли эту задачу");
        if (isHasUnclosedWorkLogs(workLogs, employee))
            throw new IllegalFields("Чтобы принять задачу, нужно завершить текущую активную");
        if (workLog.getGangLeader() != null) {
            if (!Objects.equals(employee.getLogin(), workLog.getGangLeader())) {
                throw new IllegalFields("Вы не являетесь бригадиром по данной задаче");
            }
            if (!workLog.getEmployees().stream().map(Employee::getLogin).toList().contains(workLog.getGangLeader())) {
                throw new IllegalFields("Бригадира нет среди монтажников по данной задаче");
            }
            for (Employee installer : workLog.getEmployees()) {
                List<WorkLog> installerWorkLogs = workLogRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    Join<WorkLog, Employee> employeeJoin = root.join("employees", JoinType.LEFT);
                    predicates.add(employeeJoin.in(installer));
                    predicates.add(cb.isNull(root.get("closed")));
                    return cb.and(predicates.toArray(Predicate[]::new));
                });
                if (isHasUnclosedWorkLogs(installerWorkLogs, employee)) {
                    throw new IllegalFields("Вы не можете принять задачу как бригадир т.к. у монтажника " + installer.getFullName() + " имеется не закрытая задача");
                }
            }
            for (Employee installer : workLog.getEmployees()) {
                AcceptingEntry gangAcceptingEntry = new AcceptingEntry(installer.getLogin(), installer.getTelegramUserId(), Timestamp.from(Instant.now()));
                workLog.getAcceptedEmployees().add(gangAcceptingEntry);
                workLog.getChat().getMembers().add(installer);
            }
            stompController.updateWorkLog(workLog);
            WorkLog withGangLeader = workLogRepository.save(workLog);
            Hibernate.initialize(withGangLeader.getTargetFiles());
            return withGangLeader;
        }
        workLog.getAcceptedEmployees().add(acceptingEntry);
        workLog.getChat().getMembers().add(employee);
        stompController.updateWorkLog(workLog);
        WorkLog withoutGangLeader = workLogRepository.save(workLog);
        Hibernate.initialize(withoutGangLeader.getTargetFiles());
        return withoutGangLeader;
    }

    private boolean isHasUnclosedWorkLogs(List<WorkLog> workLogs, Employee targetEmployee) {
        AcceptingEntry acceptingEntry = new AcceptingEntry(targetEmployee.getLogin(), "", Timestamp.from(Instant.now()));
        List<WorkLog> acceptedWorkLogs = workLogs.stream().filter(wl -> wl.getAcceptedEmployees().contains(acceptingEntry)).toList();
        return !acceptedWorkLogs.stream().allMatch(wl -> wl.getWorkReports().stream().anyMatch(wr -> Objects.equals(wr.getAuthor(), targetEmployee)));
    }

    public List<WorkLog> getAllByTaskId(Long taskId) {
        return workLogRepository.findAllByTask_TaskId(taskId, Sort.by(Sort.Direction.DESC, "created"));
    }

    @Transactional
    public WorkLog createReport(Employee employee, List<Message> messageList) throws EntryNotFound, IllegalFields {
        WorkLog workLog = getAcceptedWorkLogByEmployee(employee);

        StringBuilder text = new StringBuilder();

        for (Message message : messageList) {
            if (message.getText() != null) {
                text.append(message.getText()).append("\n");
            } else if (message.getCaption() != null) {
                text.append(message.getCaption()).append("\n");
            }
        }

        Timestamp timestamp = Timestamp.from(Instant.now());

        if (workLog.getGangLeader() != null) {
            if (!Objects.equals(workLog.getGangLeader(), employee.getLogin())) {
                throw new IllegalFields("Только бригадир " + workLog.getGangLeader() + " может закрыть задачу");
            }
            for (Employee installer : workLog.getEmployees()) {
                WorkReport workReport = WorkReport.builder().description(text.toString()).created(timestamp).author(installer).awaitingWriting(false).build();
                workLog.addWorkReport(workReport);
                workLog.getChat().getMembers().removeIf(member -> member.getLogin().equals(installer.getLogin()));
                notificationDispatcher.createNotification(workLog.getTask().getAllEmployeesObservers(), Notification.reportReceived(workLog, workReport));
            }
        } else {
            WorkReport workReport = WorkReport.builder().description(text.toString()).created(timestamp).author(employee).awaitingWriting(false).build();
            workLog.addWorkReport(workReport);
            workLog.getChat().getMembers().removeIf(member -> member.getLogin().equals(employee.getLogin()));
            notificationDispatcher.createNotification(workLog.getTask().getAllEmployeesObservers(), Notification.reportReceived(workLog, workReport));
        }

        stompController.updateWorkLog(workLog);
        stompController.updateChat(workLog.getChat());
        stompController.createTaskEvent(workLog.getTask().getTaskId(), taskEventDispatcher.appendEvent(TaskEvent.reportCreated(workLog.getTask(), text.toString(), employee)));
        if (workLog.getWorkReports().size() == workLog.getEmployees().size()) {
            workLog.setClosed(timestamp);
            workLog.getChat().setClosed(timestamp);
            WorkLog save = workLogRepository.save(workLog);
            taskDispatcher.close(workLog.getTask().getTaskId(), save.getCreator(), false);
            stompController.closeWorkLog(save);
            stompController.afterWorkAppend(save);
            return save;
        } else {
            return workLogRepository.save(workLog);
        }
    }

    @Transactional
    public WorkLog createReport(Employee employee) throws EntryNotFound, IllegalFields {
        WorkLog workLog = getAcceptedWorkLogByEmployee(employee);

        Timestamp timestamp = Timestamp.from(Instant.now());

        if (workLog.getGangLeader() != null) {
            if (!Objects.equals(workLog.getGangLeader(), employee.getLogin())) {
                throw new IllegalFields("Только бригадир " + workLog.getGangLeader() + " может закрыть задачу");
            }
            for (Employee installer : workLog.getEmployees()) {
                WorkReport workReport = WorkReport.builder().description("").created(timestamp).author(installer).awaitingWriting(true).build();
                workLog.addWorkReport(workReport);
                workLog.getChat().getMembers().removeIf(member -> member.getLogin().equals(installer.getLogin()));
                notificationDispatcher.createNotification(workLog.getTask().getAllEmployeesObservers(), Notification.reportReceived(workLog, workReport));
            }
        } else {
            WorkReport workReport = WorkReport.builder().description("").created(timestamp).author(employee).awaitingWriting(true).build();
            workLog.addWorkReport(workReport);
            workLog.getChat().getMembers().removeIf(member -> member.getLogin().equals(employee.getLogin()));
            notificationDispatcher.createNotification(workLog.getTask().getAllEmployeesObservers(), Notification.reportReceived(workLog, workReport));
        }

        stompController.updateWorkLog(workLog);
        stompController.updateChat(workLog.getChat());
//        stompController.createTaskEvent(workLog.getTask().getTaskId(), taskEventDispatcher.appendEvent(TaskEvent.reportCreated(workLog.getTask(), text.toString(), employee)));
        if (workLog.getWorkReports().size() == workLog.getEmployees().size()) {
            workLog.setClosed(timestamp);
            workLog.getChat().setClosed(timestamp);
            WorkLog save = workLogRepository.save(workLog);
            taskDispatcher.close(workLog.getTask().getTaskId(), save.getCreator(), false);
            stompController.closeWorkLog(save);
            stompController.afterWorkAppend(save);
            return save;
        } else {
            return workLogRepository.save(workLog);
        }
    }

    public List<WorkLog> getActive() {
        return workLogRepository.findAllByClosedIsNull(Sort.by(Sort.Direction.DESC, "created"));
    }

    public Long getActiveCount() {
        return workLogRepository.countByClosedIsNull(Sort.by(Sort.Direction.DESC, "created"));
    }

    public WorkLog getActiveByTaskId(Long taskId) throws EntryNotFound {
        return workLogRepository.findFirstByTask_TaskIdAndClosedIsNull(taskId).orElseThrow(() -> new EntryNotFound("Не найденно активного журнала работ"));
    }

    public List<WorkLog> getUncalculated() {
        return workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("closed")));
            predicates.add(cb.isFalse(root.get("calculated")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC, "created"));
    }

    @Transactional
    public void remove(WorkLog workLog) {
        workLogRepository.delete(workLog);
    }

    public WorkLog getByChatId(Long chatId) {
        return workLogRepository.findByChat_ChatId(chatId).orElseThrow(() -> new EntryNotFound("Журнал работ не найден"));
    }

    public List<WorkLog> getDoneWorks(Long wireframeId, Timestamp start, Timestamp end) {
        return workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<WorkLog, Task> taskJoin = root.join("task", JoinType.LEFT);
            Join<Task, Wireframe> modelWireframeJoin = taskJoin.join("modelWireframe", JoinType.LEFT);

            predicates.add(cb.equal(modelWireframeJoin.get("wireframeId"), wireframeId));
            predicates.add(cb.between(root.get("closed"), start, end));

            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public List<WorkLog> getUncompletedReports(Employee employee) {
        return workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<WorkLog, WorkReport> workReportJoin = root.join("workReports", JoinType.LEFT);
            predicates.add(cb.equal(workReportJoin.get("author"), employee));
            predicates.add(cb.isTrue(workReportJoin.get("awaitingWriting")));
            predicates.add(cb.isNotNull(root.get("closed")));

            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    @Transactional
    public void saveReport(WorkLog.WritingReportForm form, Employee employee) {
        WorkLog workLog = workLogRepository.findById(form.getWorkLogId()).orElseThrow(() -> new EntryNotFound("Журнал работ не найден"));
        if (workLog.getGangLeader() != null) {
            workLog.getWorkReports().forEach(wr -> {
                wr.setAwaitingWriting(false);
                wr.setDescription(form.getReportDescription());
            });
        } else {
            workLog.getWorkReports()
                    .stream()
                    .filter(wr -> Objects.equals(wr.getAuthor(), employee))
                    .filter(WorkReport::getAwaitingWriting)
                    .forEach(wr -> {
                        wr.setAwaitingWriting(false);
                        wr.setDescription(form.getReportDescription());
                    });
        }
        workLogRepository.save(workLog);
        stompController.updateWorkLog(workLog);
        stompController.updateChat(workLog.getChat());
    }

    public List<Employee> getAcceptedEmployees(Set<Employee> employees) {
        List<Employee> acceptedEmployees = new ArrayList<>();
        for (Employee employee : employees) {
            try {
                getAcceptedWorkLogByEmployee(employee);
                acceptedEmployees.add(employee);
            } catch (EntryNotFound ignored) {
            }
        }
        return acceptedEmployees;
    }

    public List<EmployeeWorkLogs> getEmployeeWorkLogList(Employee employee){
        List<WorkLog> myActiveWorkLogs = workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("closed")));
            predicates.add(cb.equal(root.get("creator"), employee));
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        Map<Set<Employee>, List<WorkLog>> employeeWorkLogs = myActiveWorkLogs.stream().collect(Collectors.groupingBy(WorkLog::getEmployees));
        List<EmployeeWorkLogs> employeeWorkLogList = new ArrayList<>();
        for (Map.Entry<Set<Employee>, List<WorkLog>> entry : employeeWorkLogs.entrySet()) {
            EmployeeWorkLogs employeeWorkLog = new EmployeeWorkLogs();
            employeeWorkLog.setEmployees(entry.getKey());
            employeeWorkLog.setActive(entry.getValue().stream().filter(WorkLog::isAllEmployeesAccepted).findFirst().orElse(null));
            employeeWorkLog.setUnactive(entry.getValue().stream().filter(WorkLog::isUnaccepted).collect(Collectors.toList()));
            employeeWorkLogList.add(employeeWorkLog);
        }

        return employeeWorkLogList;
    }

    public List<WorkLog> getAfterWork(Employee employee) {
        return workLogRepository.findAll((root, query, cb) -> {
            Join<WorkLog, Task> taskJoin = root.join("task");
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(taskJoin.get("taskStatus"), TaskStatus.PROCESSING));
            predicates.add(cb.isNotNull(root.get("closed")));
            predicates.add(cb.equal(root.get("creator"), employee));
            predicates.add(cb.isNull(root.get("taskIsClearlyCompleted")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC, "created"));
    }

    @Transactional
    public WorkLog markAsCompleted(Long id) {
        WorkLog workLog = workLogRepository.findById(id).orElseThrow(() -> new ResponseException("Журнал работ не найден"));
        workLog.setTaskIsClearlyCompleted(true);
        workLog = workLogRepository.save(workLog);

        final Task TASK = workLog.getTask();
        if (TASK.getTaskStatus() == TaskStatus.ACTIVE)
            taskDispatcher.close(TASK.getTaskId(), workLog.getCreator(), false);

        stompController.updateWorkLog(workLog);
        stompController.afterWorkRemoved(workLog.getWorkLogId(), workLog.getCreator().getLogin());
        return workLog;
    }

    @Transactional
    public WorkLog markAsUncompleted(Long id) {
        WorkLog workLog = workLogRepository.findById(id).orElseThrow(() -> new ResponseException("Журнал работ не найден"));
        workLog.setTaskIsClearlyCompleted(false);
        workLog = workLogRepository.save(workLog);

        final Task TASK = workLog.getTask();
        if (TASK.getTaskStatus() == TaskStatus.CLOSE)
            taskDispatcher.reopen(TASK.getTaskId(), workLog.getCreator());

        stompController.updateWorkLog(workLog);
        stompController.afterWorkRemoved(workLog.getWorkLogId(), workLog.getCreator().getLogin());
        return workLog;
    }

    @Transactional
    public WorkLog markAsUncompletedAndClose(Long id) {
        WorkLog workLog = workLogRepository.findById(id).orElseThrow(() -> new ResponseException("Журнал работ не найден"));
        workLog.setTaskIsClearlyCompleted(false);
        workLog = workLogRepository.save(workLog);

        final Task TASK = workLog.getTask();
        if (TASK.getTaskStatus() == TaskStatus.ACTIVE)
            taskDispatcher.close(TASK.getTaskId(), workLog.getCreator(), false);

        stompController.updateWorkLog(workLog);
        stompController.afterWorkRemoved(workLog.getWorkLogId(), workLog.getCreator().getLogin());
        return workLog;
    }

    public WorkLogTargetFile getTargetFileById(Long id) {
        return workLogTargetFileRepository.findById(id).orElseThrow(() -> new ResponseException("Файл не найден"));
    }

    @Data
    public static class EmployeeWorkLogs{
        private Set<Employee> employees;
        @Nullable
        private WorkLog active;
        private List<WorkLog> unactive;
    }
}
