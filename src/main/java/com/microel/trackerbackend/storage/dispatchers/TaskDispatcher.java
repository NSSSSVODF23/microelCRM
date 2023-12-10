package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.microel.trackerbackend.misc.DataPair;
import com.microel.trackerbackend.misc.WireframeTaskCounter;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.modules.transport.IDuration;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.dto.mapper.TaskMapper;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.TaskRepository;
import com.microel.trackerbackend.storage.repositories.TaskStageRepository;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Triplet;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
@Transactional(readOnly = true)
public class TaskDispatcher {
    private final TaskRepository taskRepository;
    private final ModelItemDispatcher modelItemDispatcher;
    private final WireframeDispatcher wireframeDispatcher;
    private final CommentDispatcher commentDispatcher;
    private final TaskStageRepository taskStageRepository;
    private final DepartmentDispatcher departmentDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final WorkLogDispatcher workLogDispatcher;
    private final TaskTagDispatcher taskTagDispatcher;
    private final NotificationDispatcher notificationDispatcher;
    private final StompController stompController;

    public TaskDispatcher(TaskRepository taskRepository, ModelItemDispatcher modelItemDispatcher,
                          WireframeDispatcher wireframeDispatcher, CommentDispatcher commentDispatcher,
                          TaskStageRepository taskStageRepository, DepartmentDispatcher departmentDispatcher,
                          EmployeeDispatcher employeeDispatcher, WorkLogDispatcher workLogDispatcher,
                          TaskTagDispatcher taskTagDispatcher, AddressDispatcher addressDispatcher,
                          @Lazy NotificationDispatcher notificationDispatcher, StompController stompController) {
        this.taskRepository = taskRepository;
        this.modelItemDispatcher = modelItemDispatcher;
        this.wireframeDispatcher = wireframeDispatcher;
        this.commentDispatcher = commentDispatcher;
        this.taskStageRepository = taskStageRepository;
        this.departmentDispatcher = departmentDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.taskTagDispatcher = taskTagDispatcher;
        this.notificationDispatcher = notificationDispatcher;
        this.stompController = stompController;
    }

    @Async
    @Transactional
    @Scheduled(cron = "0 * * ? * *")
    public void processingScheduledTasks() {
        long currentMillis = Instant.now().toEpochMilli();
        long extraSeconds = currentMillis % 60000;
        long delta = currentMillis - extraSeconds;
        Timestamp endOfMinute = Timestamp.from(Instant.ofEpochMilli(delta + 60000L));

        List<Task> tasks = taskRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(cb.lessThan(root.get("actualFrom"), endOfMinute), cb.lessThan(root.get("actualTo"), endOfMinute)));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC, "actualFrom", "actualTo"));

        for (Task task : tasks) {
            Set<Employee> recipient = task.getAllEmployeesObservers();
            if (task.getActualFrom() != null) {
                notificationDispatcher.createNotification(recipient, Notification.taskHasBecomeActual(task));
                task.setActualFrom(null);
                stompController.updateTask(taskRepository.save(task));
            } else if (task.getActualTo() != null) {
                notificationDispatcher.createNotification(recipient, Notification.taskExpired(task));
                task.setActualTo(null);
                stompController.updateTask(taskRepository.save(task));
            }
        }
    }

    @Transactional
    public Task createTask(Task.CreationBody body, Employee employee) throws IllegalFields, EntryNotFound {
        return createTask(body, Timestamp.from(Instant.now()), employee);
    }

    @Transactional
    public Task createTask(Task.CreationBody body, Timestamp timestamp, Employee employee) throws IllegalFields, EntryNotFound {
        // Создаем временный объект задачи
        Task createdTask = new Task();
        createdTask.setComments(new ArrayList<>());
        // Устанавливаем время создания задачи
        createdTask.setCreated(timestamp);
        // Устанавливаем время обновления задачи
        createdTask.setUpdated(timestamp);
        // Устанавливаем автора задачи
        createdTask.setCreator(employee);

        //Проверяем есть ли установленный шаблон в запросе
        if (body.getWireframeId() == null) throw new IllegalFields("Не установлен шаблон для создания задачи");
        // Получаем шаблон задачи из бд по идентификатору и устанавливаем его в createdTask
        Wireframe wireframe = wireframeDispatcher.getWireframeById(body.getWireframeId(), false);
        // Если wireframe null то выбрасываем исключение
        if (wireframe == null) throw new IllegalFields("В базе данных не найден шаблон для создания задачи");

        // Получаем список наблюдателей задачи по-умолчанию из шаблона
        List<DefaultObserver> defaultObservers = body.getObservers();

        if(defaultObservers != null) {
            // Выбираем из базы данных действующих наблюдателей задачи
            List<Employee> employeesObservers = employeeDispatcher.getByIdSet(DefaultObserver.getSetOfEmployees(defaultObservers).stream().map(Employee::getLogin).collect(Collectors.toSet()));
            List<Department> departmentsObservers = departmentDispatcher.getByIdSet(DefaultObserver.getSetOfDepartments(defaultObservers).stream().map(Department::getDepartmentId).collect(Collectors.toSet()));

            // Устанавливаем сотрудников как наблюдателей задачи
            createdTask.setEmployeesObservers(employeesObservers);
            // Устанавливаем отделы как наблюдателей задачи
            createdTask.setDepartmentsObservers(departmentsObservers);
        }

        if(body.getType() == null){
            createdTask.setCurrentStage(wireframe.getFirstStage());
        }else{
            TaskStage taskStage = taskStageRepository.findFirstByStageId(body.getType()).orElse(null);
            if(taskStage == null){
                createdTask.setCurrentStage(wireframe.getFirstStage());
            }else{
                createdTask.setCurrentStage(taskStage);
            }
        }

        // Устанавливаем статус задачи как активная
        createdTask.setTaskStatus(TaskStatus.ACTIVE);

        // Устанавливаем шаблон задачи
        createdTask.setModelWireframe(wireframe);

        //Подготавливаем данные в задаче для сохранения
        List<ModelItem> modelItems = modelItemDispatcher.prepareModelItems(ModelItemDispatcher.cleanToCreate(body.getFields()));

        // Устанавливаем поля задачи
        createdTask.setFields(modelItems);

        // Получаем все дочерние задачи из бд по идентификатору и устанавливаем их в createdTask
        if (body.getChildId() != null) {

            Task childrenFromDB = taskRepository.findById(body.getChildId()).orElseThrow(() -> new EntryNotFound("Дочерняя задача не найдена в базе данных"));
            createdTask.setChildren(Stream.of(childrenFromDB).collect(Collectors.toList()));
            return taskRepository.save(createdTask);
        }

        // Получаем родительскую задачу из бд по идентификатору и устанавливаем её в createdTask
        if (body.getParentId() != null) {

            Task parentFromDB = taskRepository.findById(body.getParentId()).orElseThrow(() -> new EntryNotFound("Родительская задача не найдена в базе данных"));

            createdTask.setParent(parentFromDB.getTaskId());
            parentFromDB.getChildren().add(createdTask);
            taskRepository.save(parentFromDB);
            return taskRepository.save(createdTask);
        }

        if (body.getInitialComment() != null && !body.getInitialComment().trim().isBlank()) {
            Comment initialComment = Comment.builder()
                    .deleted(false)
                    .edited(false)
                    .created(Timestamp.from(Instant.now()))
                    .creator(employee)
                    .message(body.getInitialComment())
                    .replyComment(null)
                    .attachments(new ArrayList<>())
                    .parent(null)
                    .build();
            createdTask.appendComment(initialComment);
            createdTask.setLastComment(initialComment);
        }

        if(body.getTags() != null && taskTagDispatcher.valid(body.getTags()))
            createdTask.setTags(body.getTags());

        Task task = taskRepository.save(createdTask);
        Long wireframeId = task.getModelWireframe().getWireframeId();

        task.getAllEmployeesObservers().forEach(observer -> {
            Long incomingTasksCount = getIncomingTasksCount(observer, wireframeId);
            Map<String, Long> incomingTasksCountByStages = getIncomingTasksCountByStages(observer, wireframeId);
            stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
            Map<Long, Map<Long, Long>> incomingTasksCountByTags = getIncomingTasksCountByTags(observer);
            stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
        });

        Long tasksCount = getTasksCount(task.getModelWireframe().getWireframeId());
        Map<String, Long> tasksCountByStages = getTasksCountByStages(wireframeId);
        stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
        Map<Long, Map<Long, Long>> tasksCountByTags = getTasksCountByTags();
        stompController.updateTagTaskCounter(tasksCountByTags);

        return task;
    }

    public Page<Task> getTasks(Integer page, Integer limit, @Nullable List<TaskStatus> status, @Nullable String stage, @Nullable Set<Long> template,
                               @Nullable List<FilterModelItem> filters, @Nullable String commonFilteringString, @Nullable String taskCreator,
                               @Nullable DateRange creationRange, @Nullable Set<Long> filterTags, @Nullable Set<Long> exclusionIds, @Nullable Employee employeeTask) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isEmpty()) predicates.add(root.get("taskStatus").in(status));
            if (stage != null && !stage.isBlank()) predicates.add(cb.equal(root.join("currentStage").get("stageId"), stage));
            if (template != null && !template.isEmpty())
                predicates.add(root.join("modelWireframe").get("wireframeId").in(template));
            if (filters != null && !filters.isEmpty()) {
                List<FilterModelItem> filterModelItems = filters.stream().filter(f -> !f.getValue().isNull() && !(f.getValue().isArray() && ((ArrayNode) f.getValue()).isEmpty())).toList();
                if(!filterModelItems.isEmpty())
                    predicates.add(root.get("taskId").in(modelItemDispatcher.getTaskIdsByFilters(filterModelItems)));
            }
            if (commonFilteringString != null && !commonFilteringString.isBlank()) {
                CriteriaBuilder.In<Long> inCauseTaskId = cb.in(root.get("taskId"));
                modelItemDispatcher.getTaskIdsByGlobalSearch(commonFilteringString).forEach(inCauseTaskId::value);
                commentDispatcher.getTaskIdsByGlobalSearch(commonFilteringString).forEach(inCauseTaskId::value);
                predicates.add(inCauseTaskId);
            }

            if (exclusionIds != null && !exclusionIds.isEmpty())
                predicates.add(cb.not(root.get("taskId").in(exclusionIds)));
            if (filterTags != null && !filterTags.isEmpty())
                predicates.add(root.join("tags", JoinType.INNER).get("taskTagId").in(filterTags));

            if (taskCreator != null && !taskCreator.isBlank())
                predicates.add(cb.equal(root.join("creator").get("login"), taskCreator));
            if (creationRange != null && creationRange.getStart() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("created"), creationRange.getStart()));
            if (creationRange != null && creationRange.getEnd() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("created"), creationRange.getEnd()));

            if (employeeTask != null) {
                predicates.add(cb.or(
                        root.join("employeesObservers", JoinType.LEFT).get("login").in(employeeTask.getLogin()),
                        root.join("departmentsObservers", JoinType.LEFT).join("employees", JoinType.LEFT).get("login").in(employeeTask.getLogin())));
                predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            }

            predicates.add(cb.equal(root.get("deleted"), false));

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "created")));
    }

    public Task getTask(Long id) throws EntryNotFound {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) throw new EntryNotFound("Задача с идентификатором " + id + " не найдена");
        return task;
    }

    @Transactional
    public Task unsafeSave(TaskDto task) {
        return taskRepository.save(TaskMapper.fromDto(task));
    }

    @Transactional
    public Task unsafeSave(Task task) {
        return taskRepository.save(task);
    }

    @Transactional
    public Task deleteTask(Long id) throws EntryNotFound {
        Task foundTask = taskRepository.findByTaskId(id).orElse(null);
        if (foundTask == null) throw new EntryNotFound();
        foundTask.setDeleted(true);
        foundTask.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(foundTask);
    }

    public Page<Task> getActiveTasksByStage(Long templateId, String stageId, Long offset, Integer limit) {
        return taskRepository.findAll(((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("modelWireframe").get("wireframeId"), templateId));
            if (stageId == null) predicates.add(cb.isNull(root.join("currentStage", JoinType.LEFT)));
            else predicates.add(cb.equal(root.join("currentStage").get("stageId"), stageId));
            Path<TaskStatus> taskStatus = root.get("taskStatus");
            predicates.add(cb.or(cb.equal(taskStatus, TaskStatus.ACTIVE), cb.equal(taskStatus, TaskStatus.PROCESSING)));
            predicates.add(cb.equal(root.get("deleted"), false));
            return cb.and(predicates.toArray(Predicate[]::new));
        }), new OffsetPageable(offset, limit, Sort.by(Sort.Direction.DESC, "created")));
    }

    @Transactional
    public Task changeTaskStage(Long taskId, String stageId) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        TaskStage stage = taskStageRepository.findById(stageId).orElse(null);
        if (stage == null) throw new EntryNotFound("Не найдена стадия задачи с идентификатором " + stageId);
        task.setCurrentStage(stage);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public List<Long> getActiveTaskIdsByStage(Long templateId, String stageId) {
        List<Task> tasks = taskRepository.findAll(((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("modelWireframe").get("wireframeId"), templateId));
            if (stageId == null) predicates.add(cb.isNull(root.join("currentStage", JoinType.LEFT)));
            else predicates.add(cb.equal(root.join("currentStage").get("stageId"), stageId));
            Path<TaskStatus> taskStatus = root.get("taskStatus");
            predicates.add(cb.or(cb.equal(taskStatus, TaskStatus.ACTIVE), cb.equal(taskStatus, TaskStatus.PROCESSING)));
            predicates.add(cb.equal(root.get("deleted"), false));
            return cb.and(predicates.toArray(Predicate[]::new));
        }));
        return tasks.stream().map(Task::getTaskId).collect(Collectors.toList());
    }

    @Transactional
    public WorkLog assignInstallers(Long taskId, WorkLog.AssignBody body, Employee creator) throws EntryNotFound, IllegalFields {
        WorkLog workLog = null;
        try {
            Task task = taskRepository.findByTaskId(taskId).orElse(null);
            if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
            WorkLog existed = workLogDispatcher.getActiveWorkLogByTask(task).orElse(null);
            if (existed != null) throw new IllegalFields("Задаче уже назначены другие монтажники");
            workLog = workLogDispatcher.createWorkLog(task, body, creator);
            task.setTaskStatus(TaskStatus.PROCESSING);
            task.setUpdated(Timestamp.from(Instant.now()));
            taskRepository.save(task);

            Long wireframeId = task.getModelWireframe().getWireframeId();

            task.getAllEmployeesObservers().forEach(observer -> {
                Long incomingTasksCount = getIncomingTasksCount(observer, wireframeId);
                Map<String, Long> incomingTasksCountByStages = getIncomingTasksCountByStages(observer, wireframeId);
                stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
                Map<Long, Map<Long, Long>> incomingTasksCountByTags = getIncomingTasksCountByTags(observer);
                stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
            });

            Long tasksCount = getTasksCount(task.getModelWireframe().getWireframeId());
            Map<String, Long> tasksCountByStages = getTasksCountByStages(wireframeId);
            stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
            Map<Long, Map<Long, Long>> tasksCountByTags = getTasksCountByTags();
            stompController.updateTagTaskCounter(tasksCountByTags);

            return workLog;
        }catch (Exception e){
            if (workLog != null) workLogDispatcher.remove(workLog);
            throw e;
        }
    }

    @Transactional
    public void abortAssignation(Long taskId) {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        workLogDispatcher.getActiveWorkLogByTask(task).ifPresent(workLogDispatcher::remove);
        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setUpdated(Timestamp.from(Instant.now()));
        taskRepository.save(task);
        Long wireframeId = task.getModelWireframe().getWireframeId();

        task.getAllEmployeesObservers().forEach(observer -> {
            Long incomingTasksCount = getIncomingTasksCount(observer, wireframeId);
            Map<String, Long> incomingTasksCountByStages = getIncomingTasksCountByStages(observer, wireframeId);
            stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
            Map<Long, Map<Long, Long>> incomingTasksCountByTags = getIncomingTasksCountByTags(observer);
            stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
        });

        Long tasksCount = getTasksCount(task.getModelWireframe().getWireframeId());
        Map<String, Long> tasksCountByStages = getTasksCountByStages(wireframeId);
        stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
        Map<Long, Map<Long, Long>> tasksCountByTags = getTasksCountByTags();
        stompController.updateTagTaskCounter(tasksCountByTags);
    }

    @Transactional
    public WorkLog forceCloseWorkLog(Long taskId, String reasonOfClosing, Employee employeeFromRequest) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        WorkLog workLog = workLogDispatcher.getActiveWorkLogByTask(task).orElse(null);

        if (workLog == null) throw new EntryNotFound("Не найдено ни одного журнала работ для принудительного закрытия");

        Timestamp timestamp = Timestamp.from(Instant.now());

        workLog.setIsForceClosed(true);
        workLog.setForceClosedReason(reasonOfClosing);
        workLog.setClosed(timestamp);
        workLog.getChat().setClosed(timestamp);

        Task workLogTask = workLog.getTask();

        workLogTask.setTaskStatus(TaskStatus.ACTIVE);
        workLogTask.setUpdated(timestamp);

        WorkLog save = workLogDispatcher.save(workLog);

        Long wireframeId = task.getModelWireframe().getWireframeId();

        task.getAllEmployeesObservers().forEach(observer -> {
            Long incomingTasksCount = getIncomingTasksCount(observer, wireframeId);
            Map<String, Long> incomingTasksCountByStages = getIncomingTasksCountByStages(observer, wireframeId);
            stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
            Map<Long, Map<Long, Long>> incomingTasksCountByTags = getIncomingTasksCountByTags(observer);
            stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
        });

        Long tasksCount = getTasksCount(task.getModelWireframe().getWireframeId());
        Map<String, Long> tasksCountByStages = getTasksCountByStages(wireframeId);
        stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
        Map<Long, Map<Long, Long>> tasksCountByTags = getTasksCountByTags();
        stompController.updateTagTaskCounter(tasksCountByTags);

        return save;
    }

    @Transactional
    public Task changeTaskObservers(Long id, Set<Long> departmentResponsibilities, Set<String> personalResponsibilities) throws EntryNotFound {
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);

        try {
            if (!personalResponsibilities.contains(Objects.requireNonNull(task.getResponsible()).getLogin())) {
                personalResponsibilities.add(task.getResponsible().getLogin());
            }
        } catch (NullPointerException ignored) {
        }

        task.setDepartmentsObservers(departmentDispatcher.getByIdSet(departmentResponsibilities));
        task.setEmployeesObservers(employeeDispatcher.getByIdSet(personalResponsibilities));
        task.setUpdated(Timestamp.from(Instant.now()));

        return taskRepository.save(task);
    }

    @Transactional
    public Pair<Task, Task> unlinkFromParent(Long taskId) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        Task parentTask = taskRepository.findByTaskId(task.getParent()).orElse(null);
        if (parentTask == null)
            throw new EntryNotFound("Не найдена родительская задача с идентификатором " + task.getParent());
        parentTask.getChildren().removeIf(t -> t.getTaskId().equals(task.getTaskId()));
        task.setParent(null);
        parentTask.setUpdated(Timestamp.from(Instant.now()));
        task.setUpdated(Timestamp.from(Instant.now()));
        return Pair.of(taskRepository.save(task), taskRepository.save(parentTask));
    }

    @Transactional
    public Triplet<Task, Task, Task> changeLinkToParentTask(Long taskId, Long parentTaskId) throws EntryNotFound, IllegalFields {
        // Find target task in db
        Task targetTask = taskRepository.findByTaskId(taskId).orElse(null);
        if (targetTask == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        // Find parent task in db
        Task parentTask = taskRepository.findByTaskId(parentTaskId).orElse(null);
        if (parentTask == null)
            throw new EntryNotFound("Не найдена родительская задача с идентификатором " + parentTaskId);

        Task rootTask = getRootTask(parentTaskId);
        // We check if there is a root task with the identifier taskId in the task chain and disconnect the parentTask from the parent task in order to avoid looping
        if (checkTaskChainForPresenceOfId(rootTask, taskId)) unlinkFromParent(parentTaskId);

        // Check if target task is a child of parent task
        if (parentTask.getChildren().contains(targetTask) || Objects.equals(targetTask.getParent(), parentTaskId))
            throw new IllegalFields("Задача уже является родительской");

        // Check if parent task is a child of target task
        if (targetTask.getChildren().contains(parentTask) || Objects.equals(parentTask.getParent(), taskId))
            throw new IllegalFields("Задача является дочерней целевой задачи");


        // Check if target task is already linked to parent task
        Task previousParent = taskRepository.findByTaskId(targetTask.getParent()).orElse(null);
        if (previousParent != null) {
            previousParent.getChildren().removeIf(t -> t.getTaskId().equals(targetTask.getTaskId()));

            targetTask.setParent(parentTaskId);
            parentTask.getChildren().add(targetTask);

            return Triplet.with(taskRepository.save(targetTask), taskRepository.save(parentTask), taskRepository.save(previousParent));
        }

        targetTask.setParent(parentTaskId);
        parentTask.getChildren().add(targetTask);
        targetTask.setUpdated(Timestamp.from(Instant.now()));
        parentTask.setUpdated(Timestamp.from(Instant.now()));

        return Triplet.with(taskRepository.save(targetTask), taskRepository.save(parentTask), null);
    }

    @Transactional
    public Triplet<Task, List<Task>, List<Pair<Task, Task>>> appendLinksToChildrenTask(Long taskId, Set<Long> childIds) throws EntryNotFound {
        Task targetTask = taskRepository.findByTaskId(taskId).orElse(null);
        if (targetTask == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);

        List<Task> newChildTasks = taskRepository.findAllById(childIds);
        List<Pair<Task, Task>> previousParentsChildTasks = new ArrayList<>();

        for (Task child : newChildTasks) {
            if (child.getParent() != null) {
                Task previousParent = taskRepository.findByTaskId(child.getParent()).orElse(null);
                if (previousParent == null)
                    throw new EntryNotFound("Не найдена родительская задача с идентификатором " + child.getParent());
                previousParent.getChildren().removeIf(t -> t.getTaskId().equals(child.getTaskId()));
                previousParent.setUpdated(Timestamp.from(Instant.now()));
                child.setParent(targetTask.getTaskId());
                child.setUpdated(Timestamp.from(Instant.now()));
                previousParentsChildTasks.add(Pair.of(taskRepository.save(previousParent), child));
                targetTask.getChildren().add(taskRepository.save(child));
            } else {
                child.setParent(targetTask.getTaskId());
                child.setUpdated(Timestamp.from(Instant.now()));
                targetTask.getChildren().add(taskRepository.save(child));
            }
        }
        targetTask.setUpdated(Timestamp.from(Instant.now()));

        return Triplet.with(taskRepository.save(targetTask), newChildTasks, previousParentsChildTasks);
    }

    public Task getRootTask(Long taskId) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        while (task.getParent() != null) {
            task = taskRepository.findByTaskId(task.getParent()).orElse(null);
            if (task == null) throw new EntryNotFound("Не найдена родительская задача");
        }
        return task;
    }

    private boolean checkTaskChainForPresenceOfId(Task rootTask, Long taskId) {
        if (rootTask.getTaskId().equals(taskId)) return true;
        for (Task child : rootTask.getChildren()) {
            if (checkTaskChainForPresenceOfId(child, taskId)) return true;
        }
        return false;
    }

    @Transactional
    public Task modifyTags(Long taskId, Set<TaskTag> tags) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        // Check if tags are valid
        if (!taskTagDispatcher.valid(tags)) throw new EntryNotFound("Часть тегов не найдена в базе данных");
        task.setTags(tags);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task changeTaskResponsible(Long id, Employee responsible) throws EntryNotFound, IllegalFields {
        if (responsible.getLogin() == null) throw new EntryNotFound("Не найден логин пользователя");
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        Employee employee = employeeDispatcher.getEmployee(responsible.getLogin());
        if (employee == null || employee.getDeleted())
            throw new EntryNotFound("Не найден пользователь с логином " + responsible.getLogin());
        if (employee.getOffsite()) throw new IllegalFields("Монтажник не может быть ответственным за задачу");
        task.setResponsible(employee);
        task.setUpdated(Timestamp.from(Instant.now()));
        if (!task.getEmployeesObservers().contains(employee)) task.getEmployeesObservers().add(employee);
        return taskRepository.save(task);
    }

    @Transactional
    public Task unbindTaskResponsible(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setResponsible(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task changeTaskActualFrom(Long id, Instant datetime) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        if (task.getActualTo() != null && datetime.isAfter(task.getActualTo().toInstant())) task.setActualTo(null);
        task.setActualFrom(Timestamp.from(datetime));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task changeTaskActualTo(Long id, Instant datetime) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        if (task.getActualFrom() != null && datetime.isBefore(task.getActualFrom().toInstant()))
            task.setActualFrom(null);
        task.setActualTo(Timestamp.from(datetime));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task clearTaskActualFrom(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setActualFrom(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task clearTaskActualTo(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setActualTo(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    @Transactional
    public Task close(Long id) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        if (task.getTaskStatus().equals(TaskStatus.CLOSE)) throw new IllegalFields("Задача уже закрыта");
        if (task.getTaskStatus().equals(TaskStatus.PROCESSING))
            throw new IllegalFields("Пока задача отдана монтажникам её нельзя закрыть");
        task.setTaskStatus(TaskStatus.CLOSE);
        task.setUpdated(Timestamp.from(Instant.now()));
        task.getTags().removeIf(TaskTag::getUnbindAfterClose);
        // Обновляем счетчики задач на странице
        Long wireframeId = task.getModelWireframe().getWireframeId();

        task.getAllEmployeesObservers().forEach(observer -> {
            Long incomingTasksCount = getIncomingTasksCount(observer, wireframeId);
            Map<String, Long> incomingTasksCountByStages = getIncomingTasksCountByStages(observer, wireframeId);
            stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
            Map<Long, Map<Long, Long>> incomingTasksCountByTags = getIncomingTasksCountByTags(observer);
            stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
        });

        Long tasksCount = getTasksCount(task.getModelWireframe().getWireframeId());
        Map<String, Long> tasksCountByStages = getTasksCountByStages(wireframeId);
        stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
        Map<Long, Map<Long, Long>> tasksCountByTags = getTasksCountByTags();
        stompController.updateTagTaskCounter(tasksCountByTags);

        return taskRepository.save(task);
    }

    @Transactional
    public Task reopen(Long taskId) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if (task.getTaskStatus().equals(TaskStatus.ACTIVE)) throw new IllegalFields("Задача уже активна");
        if (task.getTaskStatus().equals(TaskStatus.PROCESSING))
            throw new IllegalFields("Невозможно активировать задачу");
        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setUpdated(Timestamp.from(Instant.now()));

        taskRepository.save(task);

        Long wireframeId = task.getModelWireframe().getWireframeId();

        task.getAllEmployeesObservers().forEach(observer -> {
            Long incomingTasksCount = getIncomingTasksCount(observer, wireframeId);
            Map<String, Long> incomingTasksCountByStages = getIncomingTasksCountByStages(observer, wireframeId);
            stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
            Map<Long, Map<Long, Long>> incomingTasksCountByTags = getIncomingTasksCountByTags(observer);
            stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
        });

        Long tasksCount = getTasksCount(task.getModelWireframe().getWireframeId());
        Map<String, Long> tasksCountByStages = getTasksCountByStages(wireframeId);
        stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
        Map<Long, Map<Long, Long>> tasksCountByTags = getTasksCountByTags();
        stompController.updateTagTaskCounter(tasksCountByTags);

        return task;
    }

    @Transactional
    public Task edit(Long taskId, List<ModelItem> modelItems) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if (task.getTaskStatus().equals(TaskStatus.CLOSE)) throw new IllegalFields("Задача уже закрыта");

        // Редактируем поля задачи и сохраняем их в БД
        task.editFields(modelItemDispatcher.prepareModelItems(modelItems));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Page<Task> getIncomingTasks(Integer page, @Nullable FiltrationConditions conditions, Employee employee) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            if(conditions != null) {
                if (conditions.template != null && !conditions.template.isEmpty()) {
                    Join<Object, Object> joinWfrm = root.join("modelWireframe", JoinType.LEFT);
                    CriteriaBuilder.In<Object> inModelWireframe = cb.in(joinWfrm.get("wireframeId"));
                    conditions.template.forEach(inModelWireframe::value);
                    predicates.add(inModelWireframe);
                }
                if (conditions.stage != null && (conditions.template != null && conditions.template.size() == 1)) {
                    Join<Task, TaskStage> joinStage = root.join("currentStage", JoinType.LEFT);
                    predicates.add(cb.equal(joinStage.get("stageId"), conditions.stage));
                }
                if (conditions.tags != null && !conditions.tags.isEmpty()) {
                    Join<Task, TaskTag> joinTag = root.join("tags", JoinType.LEFT);
                    predicates.add(joinTag.get("taskTagId").in(conditions.tags));
                }
            }
            Join<Object, Object> joinDep = root.join("departmentsObservers", JoinType.LEFT);
            Join<Object, Object> joinEmp = root.join("employeesObservers", JoinType.LEFT);

            predicates.add(cb.or(joinEmp.in(employee), joinDep.join("employees", JoinType.LEFT).in(employee)));

            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, (conditions == null || conditions.limit == null) ? 15 : conditions.limit, Sort.by(Sort.Order.desc("updated").nullsLast())));
    }

    public Long getIncomingTasksCount(Employee employee) {
        return taskRepository.count((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Object, Object> joinDep = root.join("departmentsObservers", JoinType.LEFT);
            Join<Object, Object> joinEmp = root.join("employeesObservers", JoinType.LEFT);

            predicates.add(cb.or(joinEmp.in(employee), joinDep.join("employees", JoinType.LEFT).in(employee)));

            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public Long getIncomingTasksCount(Employee employee, Long wireframeId) {
        return taskRepository.count((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Department> joinDep = root.join("departmentsObservers", JoinType.LEFT);
            Join<Task, Employee> joinEmp = root.join("employeesObservers", JoinType.LEFT);
            Join<Task, Wireframe> joinWfrm = root.join("modelWireframe", JoinType.LEFT);

            predicates.add(cb.or(joinEmp.in(employee), joinDep.join("employees", JoinType.LEFT).in(employee)));
            predicates.add(cb.equal(joinWfrm.get("wireframeId"), wireframeId));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public Map<Long,Long> getTasksCountByTags(Long wireframeIds) {
        Map<Long, Long> tagsCounter = new HashMap<>();
        taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Wireframe> joinWfrm = root.join("modelWireframe", JoinType.LEFT);

            predicates.add(cb.equal(joinWfrm.get("wireframeId"), wireframeIds));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }).forEach(task -> {
            if(task == null || task.getTags() == null) return;
            task.getTags().forEach(tag -> {
                tagsCounter.compute(tag.getTaskTagId(), (key, value)->{
                    if(value == null) return 1L;
                    return value + 1L;
                });
            });
        });
        return tagsCounter;
    }
    public Map<Long,Long> getTasksCountByTags(List<Long> wireframeIds) {
        Map<Long, Long> tagsCounter = new HashMap<>();
        taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Wireframe> joinWfrm = root.join("modelWireframe", JoinType.LEFT);

            predicates.add(joinWfrm.get("wireframeId").in(wireframeIds));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }).forEach(task -> {
            if(task == null || task.getTags() == null) return;
            task.getTags().forEach(tag -> {
                tagsCounter.compute(tag.getTaskTagId(), (key, value)->{
                    if(value == null) return 1L;
                    return value + 1L;
                });
            });
        });
        return tagsCounter;
    }

    public Map<Long, Map<Long,Long>> getTasksCountByTags() {
        Map<Long, Map<Long,Long>> tagsCounter = new HashMap<>();
        taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }).forEach(task -> {
            if(task == null || task.getTags() == null) return;
            task.getTags().forEach(tag -> {
                Long wfId = task.getModelWireframe().getWireframeId();
                tagsCounter.compute(tag.getTaskTagId(), (key, value)->{
                    if(value == null) {
                        Map<Long,Long> wfMap = new HashMap<>();
                        wfMap.put(wfId, 1L);
                        return wfMap;
                    };
                    value.compute(wfId,(wireframeId, taskCount)->{
                        if(taskCount == null)
                            return 1L;

                        return taskCount + 1L;
                    });
                    return value;
                });
            });
        });
        return tagsCounter;
    }
    public Map<Long,Long> getIncomingTasksCountByTags(Employee employee, List<Long> wireframeIds) {
        Map<Long, Long> tagsCounter = new HashMap<>();
        taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Wireframe> joinWfrm = root.join("modelWireframe", JoinType.LEFT);
            Join<Task, Department> joinDep = root.join("departmentsObservers", JoinType.LEFT);
            Join<Task, Employee> joinEmp = root.join("employeesObservers", JoinType.LEFT);

            predicates.add(cb.or(joinEmp.in(employee), joinDep.join("employees", JoinType.LEFT).in(employee)));

            predicates.add(joinWfrm.get("wireframeId").in(wireframeIds));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }).forEach(task -> {
            if(task == null || task.getTags() == null) return;
            task.getTags().forEach(tag -> {
                tagsCounter.compute(tag.getTaskTagId(), (key, value)->{
                    if(value == null) return 1L;
                    return value + 1L;
                });
            });
        });
        return tagsCounter;
    }

    public Map<Long, Map<Long, Long>> getIncomingTasksCountByTags(Employee employee) {
        Map<Long, Map<Long, Long>> tagsCounter = new HashMap<>();
        taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Department> joinDep = root.join("departmentsObservers", JoinType.LEFT);
            Join<Task, Employee> joinEmp = root.join("employeesObservers", JoinType.LEFT);

            predicates.add(cb.or(joinEmp.in(employee), joinDep.join("employees", JoinType.LEFT).in(employee)));

            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }).forEach(task -> {
            if(task == null || task.getTags() == null) return;
            task.getTags().forEach(tag -> {
                Long wfId = task.getModelWireframe().getWireframeId();
                tagsCounter.compute(tag.getTaskTagId(), (key, value)->{
                    if(value == null) {
                        Map<Long,Long> wfMap = new HashMap<>();
                        wfMap.put(wfId, 1L);
                        return wfMap;
                    };
                    value.compute(wfId,(wireframeId, taskCount)->{
                       if(taskCount == null)
                           return 1L;

                       return taskCount + 1L;
                    });
                    return value;
                });
            });
        });
        return tagsCounter;
    }

    public Map<String, Long> getIncomingTasksCountByStages(Employee employee, Long wireframeId) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Wireframe> joinWfrm = root.join("modelWireframe", JoinType.LEFT);
            Join<Task, Department> joinDep = root.join("departmentsObservers", JoinType.LEFT);
            Join<Task, Employee> joinEmp = root.join("employeesObservers", JoinType.LEFT);

            predicates.add(cb.or(joinEmp.in(employee), joinDep.join("employees", JoinType.LEFT).in(employee)));

            predicates.add(cb.equal(joinWfrm.get("wireframeId"), wireframeId));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().collect(Collectors.groupingBy(task -> task.getCurrentStage().getStageId(), Collectors.counting()));
    }

    public Map<String, Long> getTasksCountByStages(Long wireframeId){
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Wireframe> joinWfrm = root.join("modelWireframe", JoinType.LEFT);

            predicates.add(cb.equal(joinWfrm.get("wireframeId"), wireframeId));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().collect(Collectors.groupingBy(task -> task.getCurrentStage().getStageId(), Collectors.counting()));
    }

    public List<Task> getScheduledTasks(Employee whose, Timestamp start, Timestamp end) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.or(
                            cb.between(root.get("actualFrom"), start, end),
                            cb.between(root.get("actualTo"), start, end)
                    )
            );

            predicates.add(
                    cb.or(
                            root.join("employeesObservers", JoinType.LEFT).get("login").in(whose.getLogin()),
                            root.join("departmentsObservers", JoinType.LEFT).join("employees", JoinType.LEFT).get("login").in(whose.getLogin())
                    )
            );

            predicates.add(cb.equal(root.get("deleted"), false));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    @Transactional
    public Task moveTaskScheduled(Long taskId, IDuration delta) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if (task.getActualFrom() != null) task.setActualFrom(delta.shift(task.getActualFrom()));
        if (task.getActualTo() != null) task.setActualTo(delta.shift(task.getActualTo()));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Long getCountByWireframe(Wireframe wireframe) {
        return taskRepository.countByModelWireframe(wireframe);
    }

    public Long getTasksCount(Long wireframeId) {
        return taskRepository.countByModelWireframe_WireframeIdAndDeletedFalseAndTaskStatusNot(wireframeId, TaskStatus.CLOSE);
    }

    public Long getTasksCount(Long wireframeId, TaskStatus taskStatus) {
        return taskRepository.countByModelWireframe_WireframeIdAndDeletedFalseAndTaskStatus(wireframeId, taskStatus);
    }

    public Page<Task> getTasksByLogin(String login, Integer page, Integer limit) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            Join<ModelItem, Task> fieldsJoin = root.join("fields", JoinType.LEFT);
            predicates.add(cb.equal(cb.lower(fieldsJoin.get("stringData")), login.toLowerCase()));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, limit, Sort.by(Sort.Order.desc("created").nullsLast(), Sort.Order.desc("updated").nullsLast())));
    }

    public WireframeDashboardStatistic getWireframeDashboardStatistic(Long wireframeId){
        Wireframe targetWireframe = wireframeDispatcher.getWireframeById(wireframeId);
        if(targetWireframe == null) throw new EntryNotFound("Не найден шаблон задач");

        WireframeDashboardStatistic statistic = new WireframeDashboardStatistic();

        List<DataPair> taskCount = new ArrayList<>();
        taskCount.add(DataPair.of("Активных", getTasksCount(wireframeId, TaskStatus.ACTIVE)));
        taskCount.add(DataPair.of("Назначенных монтажникам", getTasksCount(wireframeId, TaskStatus.PROCESSING)));

        List<DataPair> taskCountByStage = new ArrayList<>();
        Map<String, Long> mapCountByStage = getTasksCountByStages(wireframeId);
        for(TaskStage stage: targetWireframe.getStages().stream().sorted(Comparator.comparingInt(TaskStage::getOrderIndex)).toList()){
            Long count = mapCountByStage.get(stage.getStageId());
            taskCountByStage.add(DataPair.of(stage.getLabel(), count != null ? count : 0));
        }

        List<DataPair> worksDone = new ArrayList<>();

        YearMonth previousMonth = YearMonth.now();
        previousMonth = previousMonth.minusMonths(1);
        Timestamp startOfPreviousMonth = Timestamp.valueOf(previousMonth.atDay(1).atStartOfDay());
        Timestamp endOfPreviousMonth = Timestamp.valueOf(previousMonth.atEndOfMonth().atTime(23,59,59));

        LocalDate previousWeek = LocalDate.now();
        previousWeek = previousWeek.minusWeeks(1).with(DayOfWeek.MONDAY);
        Timestamp startOfPreviousWeek = Timestamp.valueOf(previousWeek.atStartOfDay());
        Timestamp endOfPreviousWeek = Timestamp.valueOf(previousWeek.plusWeeks(1).atTime(23,59,59));

        Timestamp todayStart = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp todayEnd = Timestamp.valueOf(LocalDate.now().atTime(23,59,59));

        worksDone.add(DataPair.of("Сегодня", workLogDispatcher.getDoneWorks(wireframeId, todayStart, todayEnd).size()));
        worksDone.add(DataPair.of("Предыдущая неделя", workLogDispatcher.getDoneWorks(wireframeId, startOfPreviousWeek, endOfPreviousWeek).size()));
        worksDone.add(DataPair.of("Предыдущий месяц", workLogDispatcher.getDoneWorks(wireframeId, startOfPreviousMonth, endOfPreviousMonth).size()));

        List<DataPair> taskCountByTags = new ArrayList<>();
        Map<Long, Long> mapCountByTags = getTasksCountByTags(wireframeId);
        taskTagDispatcher.getAll(null, false).stream()
                .filter(tag->mapCountByTags.containsKey(tag.getTaskTagId()))
                .forEach(tag -> taskCountByTags.add(DataPair.of(tag.getName(), mapCountByTags.get(tag.getTaskTagId()), tag.getColor())));


        statistic.setTaskCount(taskCount);
        statistic.setTaskCountByStage(taskCountByStage);
        statistic.setWorksDone(worksDone);
        statistic.setTaskCountByTags(taskCountByTags);

        return statistic;
    }

    @Getter
    @Setter
    public static class WireframeDashboardStatistic{
        private List<DataPair> taskCount;
        private List<DataPair> taskCountByStage;
        private List<DataPair> worksDone;
        private List<DataPair> taskCountByTags;
    }

    @Getter
    @Setter
    public static class FiltrationConditions {
        @Nullable
        private List<TaskStatus> status;
        @Nullable
        private Set<Long> template;
        @Nullable
        private String templateFilter;
        @Nullable
        private String searchPhrase;
        @Nullable
        private String author;
        @Nullable
        private DateRange dateOfCreation;
        @Nullable
        private Set<Long> exclusionIds;
        @Nullable
        private Set<Long> tags;
        @Nullable
        private Boolean onlyMy;
        @Nullable
        private Integer limit;
        @Nullable
        private Integer page;
        @Nullable
        private String stage;

        public void clean() {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field f : fields) {
                try {
                    Class t = f.getType();
                    Object v = f.get(this);
                    if (t == String.class && v != null) {
                        String target = (String) v;
                        if (target.isBlank() || target.equals("[]") || target.equals("null") || target.equals("undefined")) {
                            f.set(this, null);
                        }
                    }
                } catch (IllegalAccessException ignore) {

                }
            }
        }
    }
}
