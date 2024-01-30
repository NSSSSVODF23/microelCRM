package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.DataPair;
import com.microel.trackerbackend.misc.TagWithTaskCountItem;
import com.microel.trackerbackend.misc.WireframeTaskCounter;
import com.microel.trackerbackend.misc.accounting.ConnectionAgreement;
import com.microel.trackerbackend.misc.accounting.TDocumentFactory;
import com.microel.trackerbackend.misc.task.counting.AbstractTaskCounterPath;
import com.microel.trackerbackend.misc.task.filtering.fields.types.TaskFieldFilter;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.modules.transport.IDuration;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerService;
import com.microel.trackerbackend.services.external.oldtracker.requests.GetTaskRequest;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskClassOT;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.dto.mapper.TaskMapper;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.templating.*;
import com.microel.trackerbackend.storage.entities.templating.documents.ConnectionAgreementTemplate;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.OldTrackerBind;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.TaskRepository;
import com.microel.trackerbackend.storage.repositories.TaskStageRepository;
import lombok.*;
import org.hibernate.Hibernate;
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
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
    private final OldTrackerService oldTrackerService;
    private final AddressDispatcher addressDispatcher;
    private final TaskEventDispatcher taskEventDispatcher;

    public TaskDispatcher(TaskRepository taskRepository, ModelItemDispatcher modelItemDispatcher,
                          WireframeDispatcher wireframeDispatcher, CommentDispatcher commentDispatcher,
                          TaskStageRepository taskStageRepository, DepartmentDispatcher departmentDispatcher,
                          EmployeeDispatcher employeeDispatcher, WorkLogDispatcher workLogDispatcher,
                          TaskTagDispatcher taskTagDispatcher, AddressDispatcher addressDispatcher,
                          @Lazy NotificationDispatcher notificationDispatcher, StompController stompController,
                          OldTrackerService oldTrackerService, AddressDispatcher addressDispatcher1, TaskEventDispatcher taskEventDispatcher) {
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
        this.oldTrackerService = oldTrackerService;
        this.addressDispatcher = addressDispatcher1;
        this.taskEventDispatcher = taskEventDispatcher;
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
//            predicates.add(cb.or(cb.lessThan(root.get("actualFrom"), endOfMinute), cb.lessThan(root.get("actualTo"), endOfMinute)));
            predicates.add(cb.or(cb.lessThan(root.get("actualFrom"), endOfMinute)));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC, "actualFrom", "actualTo"));

        for (Task task : tasks) {
            Set<Employee> recipient = task.getAllEmployeesObservers();
            if (task.getActualFrom() != null) {
                notificationDispatcher.createNotification(recipient, Notification.taskHasBecomeActual(task));
                UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
                updateTasksCountWorker.appendPath(task);
                task.setActualFrom(null);
                Task save = taskRepository.save(task);
                updateTasksCountWorker.appendPath(save).execute(this);
                stompController.updateTask(save);
            } else if (task.getActualTo() != null) {
                notificationDispatcher.createNotification(recipient, Notification.taskExpired(task));
//                UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
//                updateTasksCountWorker.appendPath(task);
//                task.setActualTo(null);
//                Task save = taskRepository.save(task);
//                updateTasksCountWorker.appendPath(save).execute(this);
//                stompController.updateTask(save);
            }
        }
    }

    @Transactional
    public void initializeLastComments(){
        List<Task> activeTasks = taskRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("taskStatus"), TaskStatus.ACTIVE));
            predicates.add(cb.isFalse(root.get("deleted")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        for (Task task : activeTasks) {
            Hibernate.initialize(task.getLastComments());
            commentDispatcher.setLastCommentsToTask(task);
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

        // Устанавливаем статус задачи как активная
        createdTask.setTaskStatus(TaskStatus.ACTIVE);

        // Устанавливаем шаблон задачи
        createdTask.setModelWireframe(wireframe);

        //Подготавливаем данные в задаче для сохранения
        List<ModelItem> modelItems = modelItemDispatcher.prepareModelItems(ModelItemDispatcher.cleanToCreate(body.getFields()));

        // Устанавливаем поля задачи
        createdTask.setFields(modelItems);

        // Получаем список наблюдателей задачи по-умолчанию из шаблона
        List<DefaultObserver> defaultObservers = body.getObservers();

        if (defaultObservers != null) {
            // Выбираем из базы данных действующих наблюдателей задачи
            List<Employee> employeesObservers = employeeDispatcher.getByIdSet(DefaultObserver.getSetOfEmployees(defaultObservers).stream().map(Employee::getLogin).collect(Collectors.toSet()));
            List<Department> departmentsObservers = departmentDispatcher.getByIdSet(DefaultObserver.getSetOfDepartments(defaultObservers).stream().map(Department::getDepartmentId).collect(Collectors.toSet()));

            // Устанавливаем сотрудников как наблюдателей задачи
            createdTask.setEmployeesObservers(employeesObservers);
            // Устанавливаем отделы как наблюдателей задачи
            createdTask.setDepartmentsObservers(departmentsObservers);
        }

        if (body.getType() == null) {
            createdTask.setCurrentStage(wireframe.getFirstStage());
        } else {
            TaskStage taskStage = taskStageRepository.findFirstByStageId(body.getType()).orElse(null);
            if (taskStage == null) {
                createdTask.setCurrentStage(wireframe.getFirstStage());
            } else {
                createdTask.setCurrentStage(taskStage);
                if (
                        employee.isHasOldTrackerCredentials()
                                && body.getIsDuplicateInOldTracker() != null
                                && body.getIsDuplicateInOldTracker()
                                && taskStage.getOldTrackerBind() != null
                ) {
                    OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
                    Long newTaskId = requestFactory.getNewTaskId(taskStage.getOldTrackerBind().getClassId()).execute();

                    TaskClassOT taskClass = oldTrackerService.getTaskClassById(taskStage.getOldTrackerBind().getClassId());

                    List<OldTrackerRequestFactory.FieldData> fields = Stream.concat(taskClass.getStandardFieldsOnCreation().get().stream(), createdTask.getFieldsForOldTracker(taskClass).stream()).collect(Collectors.toList());

                    requestFactory.createTask(newTaskId, taskStage.getOldTrackerBind().getInitialStageId(), fields)
                            .setInitialComment(body.getInitialComment())
                            .execute();

                    requestFactory.close().execute();

                    createdTask.setOldTrackerTaskId(newTaskId);
                    createdTask.setOldTrackerTaskClassId(taskStage.getOldTrackerBind().getClassId());
                    createdTask.setOldTrackerCurrentStageId(taskStage.getOldTrackerBind().getInitialStageId());

                    System.out.println("Новый идентификатор задачи: " + newTaskId);
                }
            }
        }

        if (createdTask.getCurrentStage() != null && createdTask.getCurrentStage().getDirectories() != null && !createdTask.getCurrentStage().getDirectories().isEmpty()) {
            if (body.getDirectory() == null) {
                createdTask.setCurrentDirectory(createdTask.getCurrentStage().getDirectories().get(0));
            } else {
                createdTask.getCurrentStage().getDirectories()
                        .stream()
                        .filter(taskTypeDirectory -> taskTypeDirectory.getTaskTypeDirectoryId().equals(body.getDirectory()))
                        .findFirst()
                        .ifPresent(createdTask::setCurrentDirectory);
            }
        }

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
            createdTask.setLastComments(List.of(initialComment));
        }

        if (body.getTags() != null && taskTagDispatcher.valid(body.getTags()))
            createdTask.setTags(body.getTags());

        Task task = taskRepository.save(createdTask);
        stompController.createTask(task);
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

        UpdateTasksCountWorker.of(task).execute(this);

        return task;
    }

    public Page<Task> getTasks(Integer page, Integer limit, @Nullable FiltrationConditions conditions, @Nullable Employee employeeTask) {
        return taskRepository.findAll((root, query, cb) -> {
            if(conditions == null) return cb.and(cb.equal(root.get("deleted"), false));

            Predicate[] predicates = conditions.toPredicateList(root, query, cb, employeeTask, modelItemDispatcher, addressDispatcher, commentDispatcher);
            query.distinct(true);
            return cb.and(predicates);

        }, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "created")));
    }

    public Page<Task> getIncomingTasks(Integer page, @Nullable FiltrationConditions conditions, Employee employee) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            if(conditions != null) {
                Predicate[] conditionPredicates = conditions.toPredicateList(root, query, cb, null, modelItemDispatcher, addressDispatcher, commentDispatcher);
                query.distinct(true);
                predicates.addAll(List.of(conditionPredicates));
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
    public Task deleteTask(Long id, Employee employee) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound();
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        task.setDeleted(true);
        task.setCurrentDirectory(null);
        task.setUpdated(Timestamp.from(Instant.now()));

        if(employee.isHasOldTrackerCredentials()){
            TaskStage taskStage = task.getCurrentStage();
            if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
                requestFactory.deleteTask(task.getOldTrackerTaskId()).execute();
                requestFactory.close().execute();
            }
        }

        Task save = taskRepository.save(task);
        stompController.updateTask(task);
        updateTasksCountWorker.execute(this);

        return save;
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

        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);

        TaskStage stage = taskStageRepository.findById(stageId).orElse(null);
        if (stage == null) throw new EntryNotFound("Не найден тип задачи с идентификатором " + stageId);
        if(task.getCurrentStage().getOldTrackerBind() != null){
            if(stage.getOldTrackerBind() == null) throw new ResponseException("Невозможно изменить тип задачи, в целевом типе не задана привязка к старому трекеру");
            if(!Objects.equals(task.getCurrentStage().getOldTrackerBind().getClassId(), stage.getOldTrackerBind().getClassId())) throw new ResponseException("Невозможно изменить тип задачи, в привязках к старому трекеру заданы разные классы задач");
        }
        task.setCurrentStage(stage);
        if(stage.getDirectories() != null && !stage.getDirectories().isEmpty()){
            task.setCurrentDirectory(task.getCurrentStage().getDirectories().get(0));
        }else {
            task.setCurrentDirectory(null);
        }

        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);

        updateTasksCountWorker.appendPath(save).execute(this);

        return save;
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
            UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
            WorkLog existed = workLogDispatcher.getActiveWorkLogByTask(task).orElse(null);
            if (existed != null) throw new IllegalFields("Задаче уже назначены другие монтажники");
            workLog = workLogDispatcher.createWorkLog(task, body, creator);
            task.setTaskStatus(TaskStatus.PROCESSING);
            task.setUpdated(Timestamp.from(Instant.now()));
            task.setActualFrom(null);
            task.setActualTo(null);

            if(creator.isHasOldTrackerCredentials()){
                TaskStage taskStage = task.getCurrentStage();
                if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                    OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(creator.getOldTrackerCredentials().getUsername(), creator.getOldTrackerCredentials().getPassword());
                    TaskClassOT taskClassOT = oldTrackerService.getTaskClassById(task.getOldTrackerTaskClassId());
                    List<OldTrackerRequestFactory.FieldData> dataList = taskClassOT.getStandardFieldsOnAssignation().get(workLog.getEmployees().toArray(Employee[]::new));
                    requestFactory.changeStageTask(task.getOldTrackerTaskId(), taskStage.getOldTrackerBind().getProcessingStageId(), dataList).execute();
                    requestFactory.close().execute();
                    task.setOldTrackerCurrentStageId(taskStage.getOldTrackerBind().getProcessingStageId());
                }
            }

            stompController.updateTask(taskRepository.save(task));

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

            updateTasksCountWorker.appendPath(task).execute(this);

            return workLog;
        }catch (Exception e){
            if (workLog != null) workLogDispatcher.remove(workLog);
            throw e;
        }
    }

    @Transactional
    public void abortAssignation(Long taskId, Employee employee) {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        workLogDispatcher.getActiveWorkLogByTask(task).ifPresent(workLogDispatcher::remove);
        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setUpdated(Timestamp.from(Instant.now()));
        Long wireframeId = task.getModelWireframe().getWireframeId();

        if(employee.isHasOldTrackerCredentials()){
            TaskStage taskStage = task.getCurrentStage();
            if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
                TaskClassOT taskClassOT = oldTrackerService.getTaskClassById(taskStage.getOldTrackerBind().getClassId());
                requestFactory.changeStageTask(task.getOldTrackerTaskId(), taskStage.getOldTrackerBind().getInitialStageId(), task.getFieldsForOldTracker(taskClassOT)).execute();
                requestFactory.close().execute();
                task.setOldTrackerCurrentStageId(taskStage.getOldTrackerBind().getInitialStageId());
            }
        }

        stompController.updateTask(taskRepository.save(task));

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

        updateTasksCountWorker.appendPath(task).execute(this);
    }

    @Transactional
    public WorkLog forceCloseWorkLog(Long taskId, String reasonOfClosing, Employee employee) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
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
        stompController.afterWorkAppend(save);

        if(employee.isHasOldTrackerCredentials()){
            TaskStage taskStage = task.getCurrentStage();
            if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
                TaskClassOT taskClassOT = oldTrackerService.getTaskClassById(taskStage.getOldTrackerBind().getClassId());
                requestFactory.changeStageTask(task.getOldTrackerTaskId(), taskStage.getOldTrackerBind().getInitialStageId(), task.getFieldsForOldTracker(taskClassOT)).execute();
                requestFactory.close().execute();
                task.setOldTrackerCurrentStageId(taskStage.getOldTrackerBind().getInitialStageId());
            }
        }

        stompController.updateTask(taskRepository.save(task));
        updateTasksCountWorker.appendPath(task).execute(this);

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

        Task save = taskRepository.save(task);
        stompController.updateTask(save);

        return save;
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
        Pair<Task, Task> pair = Pair.of(taskRepository.save(task), taskRepository.save(parentTask));
        stompController.updateTask(pair.getFirst());
        stompController.updateTask(pair.getSecond());
        return pair;
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

            Triplet<Task, Task, Task> trip = Triplet.with(taskRepository.save(targetTask), taskRepository.save(parentTask), taskRepository.save(previousParent));
            stompController.updateTask(trip.getValue0());
            stompController.updateTask(trip.getValue1());
            stompController.updateTask(trip.getValue2());
            return trip;
        }

        targetTask.setParent(parentTaskId);
        parentTask.getChildren().add(targetTask);
        targetTask.setUpdated(Timestamp.from(Instant.now()));
        parentTask.setUpdated(Timestamp.from(Instant.now()));

        Triplet<Task, Task, Task> trip = Triplet.with(taskRepository.save(targetTask), taskRepository.save(parentTask), null);
        stompController.updateTask(trip.getValue0());
        stompController.updateTask(trip.getValue1());
        return trip;
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

        Triplet<Task, List<Task>, List<Pair<Task, Task>>> trip = Triplet.with(taskRepository.save(targetTask), newChildTasks, previousParentsChildTasks);
        stompController.updateTask(trip.getValue0());
        for (Task task : trip.getValue1()) {
            stompController.updateTask(task);
        }
        for (Pair<Task, Task> pair : trip.getValue2()) {
            stompController.updateTask(pair.getFirst());
        }
        return trip;
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
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        // Check if tags are valid
        if (!taskTagDispatcher.valid(tags)) throw new EntryNotFound("Часть тегов не найдена в базе данных");
        task.setTags(tags);
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        updateTasksCountWorker.appendPath(save).execute(this);
        return save;
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
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        return save;
    }

    @Transactional
    public Task unbindTaskResponsible(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setResponsible(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        return save;
    }

    @Transactional
    public Task changeTaskActualFrom(Long id, Instant datetime) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        updateTasksCountWorker.appendPath(task);
        if (task.getActualTo() != null && datetime.isAfter(task.getActualTo().toInstant())) task.setActualTo(null);
        task.setActualFrom(Timestamp.from(datetime));
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        updateTasksCountWorker.appendPath(save).execute(this);
        return save;
    }

    @Transactional
    public Task changeTaskActualTo(Long id, Instant datetime) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        updateTasksCountWorker.appendPath(task);
        if (task.getActualFrom() != null && datetime.isBefore(task.getActualFrom().toInstant()))
            task.setActualFrom(null);
        task.setActualTo(Timestamp.from(datetime));
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        updateTasksCountWorker.appendPath(save).execute(this);
        return save;
    }

    @Transactional
    public Task clearTaskActualFrom(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        updateTasksCountWorker.appendPath(task);
        task.setActualFrom(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        updateTasksCountWorker.appendPath(save).execute(this);
        return save;
    }

    @Transactional
    public Task clearTaskActualTo(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        updateTasksCountWorker.appendPath(task);
        task.setActualTo(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        updateTasksCountWorker.appendPath(save).execute(this);
        return save;
    }

    @Transactional
    public Task close(Long id, @Nullable Employee employee) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        if (task.getTaskStatus().equals(TaskStatus.CLOSE)) throw new IllegalFields("Задача уже закрыта");
        if (employee != null && task.getTaskStatus().equals(TaskStatus.PROCESSING))
            throw new IllegalFields("Пока задача отдана монтажникам её нельзя закрыть");
        task.setTaskStatus(TaskStatus.CLOSE);
        final Timestamp NOW = Timestamp.from(Instant.now());
        task.setUpdated(NOW);
        task.setClosed(NOW);
        if(task.getTags() != null) task.getTags().removeIf(TaskTag::getUnbindAfterClose);
        task.setCurrentDirectory(null);
        task.setActualFrom(null);
        task.setActualTo(null);
        // Обновляем счетчики задач на странице
        Long wireframeId = task.getModelWireframe().getWireframeId();

        if(employee == null) employee = task.getCreator();
        if(employee.isHasOldTrackerCredentials()){
            TaskStage taskStage = task.getCurrentStage();
            if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
                TaskClassOT taskClassOT = oldTrackerService.getTaskClassById(taskStage.getOldTrackerBind().getClassId());
                requestFactory.changeStageTask(task.getOldTrackerTaskId(), taskStage.getOldTrackerBind().getManualCloseStageId(), task.getFieldsForOldTracker(taskClassOT)).execute();
                requestFactory.close().execute();
                task.setOldTrackerCurrentStageId(taskStage.getOldTrackerBind().getManualCloseStageId());
            }
        }

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

        Task save = taskRepository.save(task);
        updateTasksCountWorker.appendPath(save).execute(this);
        stompController.updateTask(save);
        Set<Employee> observers = save.getAllEmployeesObservers(employee);
        notificationDispatcher.createNotification(observers, Notification.taskClosed(save, employee));
        TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.close(save, employee));
        stompController.createTaskEvent(save.getTaskId(), taskEvent);
        return save;
    }

    @Transactional
    public Task close(Long id) throws EntryNotFound, IllegalFields {
        return close(id, null);
    }

    @Transactional
    public Task reopen(Long taskId, Employee employee) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        UpdateTasksCountWorker updateTasksCountWorker = UpdateTasksCountWorker.of(task);
        if (task.getTaskStatus().equals(TaskStatus.ACTIVE)) throw new IllegalFields("Задача уже активна");
        if (task.getTaskStatus().equals(TaskStatus.PROCESSING))
            throw new IllegalFields("Невозможно активировать задачу");
        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setUpdated(Timestamp.from(Instant.now()));

        if(task.getCurrentStage() != null && task.getCurrentStage().getDirectories() != null && !task.getCurrentStage().getDirectories().isEmpty()){
            task.setCurrentDirectory(task.getCurrentStage().getDirectories().get(0));
        }

        if(employee.isHasOldTrackerCredentials()){
            TaskStage taskStage = task.getCurrentStage();
            if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
                TaskClassOT taskClassOT = oldTrackerService.getTaskClassById(taskStage.getOldTrackerBind().getClassId());
                requestFactory.changeStageTask(task.getOldTrackerTaskId(), taskStage.getOldTrackerBind().getInitialStageId(), task.getFieldsForOldTracker(taskClassOT)).execute();
                requestFactory.close().execute();
                task.setOldTrackerCurrentStageId(taskStage.getOldTrackerBind().getInitialStageId());
            }
        }

        stompController.updateTask(taskRepository.save(task));

        Set<Employee> observers = task.getAllEmployeesObservers(employee);
        notificationDispatcher.createNotification(observers, Notification.taskReopened(task, employee));
        TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.reopen(task, employee));
        stompController.createTaskEvent(taskId, taskEvent);

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
        updateTasksCountWorker.appendPath(task).execute(this);
        return task;
    }

    @Transactional
    public List<Task> moveToDirectory(MovingToDirectoryForm form) throws EntryNotFound {
        List<Task> tasks = taskRepository.findAllById(form.taskIds);
        if (tasks.size() != form.taskIds.size()) throw new EntryNotFound("Некоторые задачи не найдены");
        UpdateTasksCountWorker updateTasksCountWorker = new UpdateTasksCountWorker();
        for (Task task : tasks) {
            updateTasksCountWorker.appendPath(task);
            TaskStage currentStage = task.getCurrentStage();
            if (currentStage == null || currentStage.getDirectories() == null || currentStage.getDirectories().isEmpty())
                throw new ResponseException("Некоторые задачи не удалось переместить, отсутствует тип или категории");
            TaskTypeDirectory currentDirectory = currentStage.getDirectories().stream().filter(taskTypeDirectory -> Objects.equals(taskTypeDirectory.getTaskTypeDirectoryId(), form.directoryId)).findFirst().orElse(null);
            if (currentDirectory == null)
                throw new ResponseException("Некоторые задачи не удалось переместить, не валидная целевая категория");
            task.setCurrentDirectory(currentDirectory);
            task.setUpdated(Timestamp.from(Instant.now()));
            updateTasksCountWorker.appendPath(task);
        }
        List<Task> taskList = taskRepository.saveAll(tasks);
        for (Task task : taskList)
            stompController.updateTask(task);
        updateTasksCountWorker.execute(this);
        return taskList;
    }

    @Transactional
    public Task edit(Long taskId, List<ModelItem> modelItems, Employee employee) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if (task.getTaskStatus().equals(TaskStatus.CLOSE)) throw new IllegalFields("Задача уже закрыта");

        // Редактируем поля задачи и сохраняем их в БД
        task.editFields(modelItemDispatcher.prepareModelItems(modelItems));
        if(employee.isHasOldTrackerCredentials()){
            OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
            TaskStage taskStage = task.getCurrentStage();
            if(taskStage.getOldTrackerBind() != null && Objects.equals(taskStage.getOldTrackerBind().getClassId(), task.getOldTrackerTaskClassId())) {
                TaskClassOT taskClassOT = oldTrackerService.getTaskClassById(taskStage.getOldTrackerBind().getClassId());
                requestFactory.editTask(task.getOldTrackerTaskId(), task.getFieldsForOldTracker(taskClassOT)).execute();
                requestFactory.close().execute();
            }
        }
        task.setUpdated(Timestamp.from(Instant.now()));
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        return save;
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
        Task save = taskRepository.save(task);
        stompController.updateTask(save);
        return save;
    }

    public Long getCountByWireframe(Wireframe wireframe) {
        return taskRepository.countByModelWireframe(wireframe);
    }

    public Long getTasksCount(Long wireframeId) {
        return taskRepository.countByModelWireframe_WireframeIdAndDeletedFalseAndTaskStatusNot(wireframeId, TaskStatus.CLOSE);
    }

    public Long getTasksCount(TaskDispatcher.FiltrationConditions conditions) {
        return taskRepository.count((root, query, cb) -> {
            Predicate[] predicates = conditions.toPredicateList(root, query, cb, null, modelItemDispatcher, addressDispatcher, commentDispatcher);
            query.distinct(true);
            return cb.and(predicates);
        });
    }

    public Long getTasksCount(AbstractTaskCounterPath path) {
        return getTasksCount(path.toFiltrationCondition());
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

        DateRange lastMonth = DateRange.lastMonth();
        DateRange lastWeek = DateRange.lastWeek();
        DateRange today = DateRange.today();

        worksDone.add(DataPair.of("Сегодня", workLogDispatcher.getDoneWorks(wireframeId, today.start(), today.end()).size()));
        worksDone.add(DataPair.of("Предыдущая неделя", workLogDispatcher.getDoneWorks(wireframeId, lastWeek.start(), lastWeek.end()).size()));
        worksDone.add(DataPair.of("Предыдущий месяц", workLogDispatcher.getDoneWorks(wireframeId, lastMonth.start(), lastMonth.end()).size()));

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

    public void checkCompatibility(Long taskId, Long otTaskId, Employee employee) {
        Task targetTask = taskRepository.findByTaskId(taskId).orElse(null);
        if(targetTask == null) throw new ResponseException("Целевая задача №"+taskId+" не найдена");
        OldTrackerBind oldTrackerBind = targetTask.getCurrentStage().getOldTrackerBind();
        if(oldTrackerBind == null) throw new ResponseException("У типа задачи не установлена привязка к старому трекеру");
        if(!employee.isHasOldTrackerCredentials()) throw new ResponseException("У вас нет доступа к старому трекеру");
        OldTrackerRequestFactory oldTrackerRequestFactory  = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
        GetTaskRequest.TaskInfo otTaskInfo = oldTrackerRequestFactory.getTask(otTaskId).execute();
        oldTrackerRequestFactory.close().execute();
        TaskClassOT taskClassByName = oldTrackerService.getTaskClassByName(otTaskInfo.getClassName());
        if(!Objects.equals(oldTrackerBind.getClassId(), taskClassByName.getId())) throw new ResponseException("Класс настроенный в привязке к трекеру не совпадает с классом задачи в трекере");
    }

    @Transactional
    public void connectToOldTracker(Long taskId, Long otTaskId, Employee employee) {
        Task targetTask = taskRepository.findByTaskId(taskId).orElse(null);
        if(targetTask == null) throw new ResponseException("Целевая задача №"+taskId+" не найдена");
        OldTrackerBind oldTrackerBind = targetTask.getCurrentStage().getOldTrackerBind();
        if(oldTrackerBind == null) throw new ResponseException("У типа задачи не установлена привязка к старому трекеру");
        if(!employee.isHasOldTrackerCredentials()) throw new ResponseException("У вас нет доступа к старому трекеру");
        OldTrackerRequestFactory oldTrackerRequestFactory  = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
        GetTaskRequest.TaskInfo otTaskInfo = oldTrackerRequestFactory.getTask(otTaskId).execute();
        oldTrackerRequestFactory.close().execute();
        TaskClassOT taskClassByName = oldTrackerService.getTaskClassByName(otTaskInfo.getClassName());
        if(!Objects.equals(oldTrackerBind.getClassId(), taskClassByName.getId())) throw new ResponseException("Класс настроенный в привязке к трекеру не совпадает с классом задачи в трекере");
        targetTask.setOldTrackerTaskId(otTaskId);
        targetTask.setOldTrackerTaskClassId(taskClassByName.getId());
        targetTask.setOldTrackerCurrentStageId(otTaskInfo.getStageId());
        stompController.updateTask(taskRepository.save(targetTask));
    }

    @Transactional
    public void changeTaskStageInOldTracker(Long taskId, Integer taskStageId, Employee employee) {
        Task targetTask = taskRepository.findByTaskId(taskId).orElse(null);
        if(targetTask == null) throw new ResponseException("Целевая задача №"+taskId+" не найдена");
        OldTrackerBind oldTrackerBind = targetTask.getCurrentStage().getOldTrackerBind();
        if(oldTrackerBind == null) throw new ResponseException("У типа задачи не установлена привязка к старому трекеру");
        if(!employee.isHasOldTrackerCredentials()) throw new ResponseException("У вас нет доступа к старому трекеру");
        OldTrackerRequestFactory oldTrackerRequestFactory  = new OldTrackerRequestFactory(employee.getOldTrackerCredentials().getUsername(), employee.getOldTrackerCredentials().getPassword());
        TaskClassOT taskClassById = oldTrackerService.getTaskClassById(oldTrackerBind.getClassId());
        if(!taskClassById.isStageExist(taskStageId)) throw new ResponseException("Не существует стадии №"+taskStageId+" у класса задачи в трекере");
        oldTrackerRequestFactory.changeStageTask(targetTask.getOldTrackerTaskId(), taskStageId, targetTask.getFieldsForOldTracker(taskClassById)).execute();
        oldTrackerRequestFactory.close().execute();
        targetTask.setOldTrackerCurrentStageId(taskStageId);
        stompController.updateTask(taskRepository.save(targetTask));
    }

    public void getDocumentTemplate(Long taskId, Long documentTemplateId, HttpServletResponse response) {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if(task == null) throw new ResponseException("Задача не найдена");
        if(task.getModelWireframe().getDocumentTemplates() == null || task.getModelWireframe().getDocumentTemplates().isEmpty())
            throw new ResponseException("Отсутствуют шаблоны документов для данной задачи");

        DocumentTemplate foundedDocumentTemplate = task.getModelWireframe().getDocumentTemplates().stream()
                .filter(documentTemplate -> Objects.equals(documentTemplate.getDocumentTemplateId(), documentTemplateId)).findFirst().orElse(null);
        if(foundedDocumentTemplate == null) throw new ResponseException("Шаблон документа не найден");

        if(foundedDocumentTemplate instanceof ConnectionAgreementTemplate connectionAgreementTemplate){
            AtomicReference<String> loginData = new AtomicReference<>();
            AtomicReference<String> fullNameData = new AtomicReference<>();
            AtomicReference<String> dateOfBirthData = new AtomicReference<>();
            AtomicReference<String> regionOfBirthData = new AtomicReference<>();
            AtomicReference<String> cityOfBirthData = new AtomicReference<>();
            AtomicReference<PassportDetails> passportDetailsData = new AtomicReference<>();
            AtomicReference<Address> addressData = new AtomicReference<>();
            AtomicReference<String> phoneData = new AtomicReference<>();
            AtomicReference<String> passwordData = new AtomicReference<>();
            AtomicReference<String> tariffData = new AtomicReference<>();

            if(connectionAgreementTemplate.getLoginFieldId() != null){
                task.getFieldByIdAndType(connectionAgreementTemplate.getLoginFieldId(), WireframeFieldType.LOGIN).ifPresent(modelItem -> {
                    loginData.set(modelItem.getTextRepresentation());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getFullNameFieldId(), WireframeFieldType.SMALL_TEXT).ifPresent(modelItem -> {
                    fullNameData.set(modelItem.getTextRepresentation());
                });
                // TODO Добавить поле даты
                task.getFieldByIdAndType(connectionAgreementTemplate.getDateOfBirthFieldId(), WireframeFieldType.SMALL_TEXT).ifPresent(modelItem -> {
                    dateOfBirthData.set(modelItem.getTextRepresentation());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getRegionOfBirthFieldId(), WireframeFieldType.SMALL_TEXT).ifPresent(modelItem -> {
                    regionOfBirthData.set(modelItem.getTextRepresentation());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getCityOfBirthFieldId(), WireframeFieldType.SMALL_TEXT).ifPresent(modelItem -> {
                    cityOfBirthData.set(modelItem.getTextRepresentation());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getPassportDetailsFieldId(), WireframeFieldType.PASSPORT_DETAILS).ifPresent(modelItem -> {
                    passportDetailsData.set(modelItem.getPassportDetailsData());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getAddressFieldId(), WireframeFieldType.ADDRESS).ifPresent(modelItem -> {
                    addressData.set(modelItem.getAddressData());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getPhoneFieldId(), WireframeFieldType.PHONE_ARRAY).ifPresent(modelItem -> {
                    phoneData.set(modelItem.getTextRepresentation());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getPasswordFieldId(), WireframeFieldType.SMALL_TEXT).ifPresent(modelItem -> {
                    passwordData.set(modelItem.getTextRepresentation());
                });
                task.getFieldByIdAndType(connectionAgreementTemplate.getTariffFieldId(), WireframeFieldType.SMALL_TEXT).ifPresent(modelItem -> {
                    tariffData.set(modelItem.getTextRepresentation());
                });
            }

            ConnectionAgreement connectionAgreement = TDocumentFactory.createConnectionAgreement(loginData.get(), fullNameData.get(), dateOfBirthData.get(), regionOfBirthData.get(), cityOfBirthData.get(),
                    passportDetailsData.get(), addressData.get(), phoneData.get(), passwordData.get(), tariffData.get());

            connectionAgreement.sendByResponse(response);
        }
    }

    public List<TagWithTaskCountItem> getTagsListFromCatalog(FiltrationConditions condition) {
        List<Task> taskList = taskRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = Arrays.stream(condition.toPredicateList(root, query, cb, null, modelItemDispatcher, addressDispatcher, commentDispatcher)).collect(Collectors.toList());
//            Join<Task, TaskTag> taskTagJoin = root.join("tags");
//            predicates.add(cb.isNotNull(taskTagJoin));
            predicates.add(cb.isNotEmpty(root.get("tags")));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        Set<TaskTag> setOfTags = taskList.stream().flatMap(task -> task.getTags().stream()).collect(Collectors.toSet());
        return setOfTags.stream().map(taskTag -> {
            long count = taskList.stream().filter(task -> task.getTags().contains(taskTag)).count();
            return new TagWithTaskCountItem(taskTag.getTaskTagId(), taskTag.getName(), taskTag.getColor(), count);
        }).sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).toList();
    }

    @Transactional
    public void restoreTasksToOriginalDirectory(Wireframe wireframe){
        if(wireframe.getStages() != null){
            for(TaskStage taskStage : wireframe.getStages()){
                if(taskStage.getDirectories() != null && !taskStage.getDirectories().isEmpty()){
                    List<Task> tasks = taskRepository.findAll((root, query, cb) -> {
                        query.distinct(true);
                        return cb.and(
                                cb.isNull(root.get("currentDirectory")),
                                cb.isFalse(root.get("deleted")),
                                cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE),
                                cb.equal(root.join("currentStage").get("stageId"), taskStage.getStageId())
                        );
                    });
                    UpdateTasksCountWorker updateTasksCountWorker = new UpdateTasksCountWorker();
                    for (Task task : tasks) {
                        updateTasksCountWorker.appendPath(task);
                        task.setCurrentDirectory(taskStage.getDirectories().get(0));
                        updateTasksCountWorker.appendPath(task);
                    }
                    List<Task> taskList = taskRepository.saveAll(tasks);
                    for (Task task : taskList)
                        stompController.updateTask(task);
                    updateTasksCountWorker.execute(this);
                }
            }
        }
    }

    @Transactional
    public void removeLinksToDirectories(List<Long> removingDirectories) {
        List<Task> tasks = taskRepository.findAll((root, query, cb) ->
                cb.and(root.join("currentDirectory", JoinType.LEFT)
                        .get("taskTypeDirectoryId")
                        .in(removingDirectories))
        );
//        tasks.forEach(task -> task.setCurrentDirectory(null));
        taskRepository.saveAll(tasks);
    }

    public List<TaskStage> getAvailableTaskTypesToChange(Long id) {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if(task == null) throw new ResponseException("Задача не найдена");
        return task.getModelWireframe().getStages().stream().filter(taskStage -> !Objects.equals(taskStage.getStageId(), task.getCurrentStage().getStageId())).toList();
    }

    public List<TaskTypeDirectory> getAvailableTaskDirectoryToChange(Long id) {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if(task == null) throw new ResponseException("Задача не найдена");
        return task.getCurrentStage().getDirectories().stream().filter(taskTypeDirectory -> !Objects.equals(taskTypeDirectory.getTaskTypeDirectoryId(), task.getCurrentDirectory().getTaskTypeDirectoryId())).toList();
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
        private List<FilterModelItem> templateFilter;
        @Nullable
        private List<TaskFieldFilter> fieldFilters;
        @Nullable
        private String searchPhrase;
        @Nullable
        private String author;
        @Nullable
        private String assignedEmployee;
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
        @Nullable
        private Long directory;
        @Nullable
        private SchedulingType schedulingType;
        @Nullable
        private DateRange dateOfClose;
        @Nullable
        private DateRange actualFrom;
        @Nullable
        private DateRange actualTo;

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

        public Predicate[] toPredicateList(Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb, Employee employeeTask,
                                           ModelItemDispatcher mid, AddressDispatcher adrd, CommentDispatcher comd){
            ArrayList<Predicate> predicates = new ArrayList<>();

            Join<Task, Wireframe> wireframeJoin = root.join("modelWireframe", JoinType.LEFT);
            Join<Task, TaskStage> taskStageJoin = root.join("currentStage", JoinType.LEFT);
            Join<Task, TaskTypeDirectory> taskTypeDirectoryJoin = root.join("currentDirectory", JoinType.LEFT);
            Join<Task, WorkLog> workLogJoin = root.join("workLogs", JoinType.LEFT);
            Join<WorkLog, Employee> workLogEmployeesJoin = workLogJoin.join("employees", JoinType.LEFT);
            Join<Task, ModelItem> fieldJoin = root.join("fields", JoinType.INNER);

            predicates.add(cb.isFalse(wireframeJoin.get("deleted")));

            if (status != null && !status.isEmpty()) {
                predicates.add(root.get("taskStatus").in(status));
            }
            if (stage != null && !stage.isBlank()) predicates.add(cb.equal(taskStageJoin.get("stageId"), stage));
            if (template != null && !template.isEmpty())
                predicates.add(wireframeJoin.get("wireframeId").in(template));
            if (directory != null)
                if(directory == 0L){
                    predicates.add(cb.isNull(taskTypeDirectoryJoin));
                }else{
                    predicates.add(cb.equal(taskTypeDirectoryJoin.get("taskTypeDirectoryId"), directory));
                }
            if (templateFilter != null && !templateFilter.isEmpty()) {
                List<FilterModelItem> nonEmptyFilters = templateFilter.stream().filter(FilterModelItem::isNotEmpty).toList();
                if(!nonEmptyFilters.isEmpty())
                    predicates.add(root.get("taskId").in(mid.getTaskIdsByFilters(nonEmptyFilters)));
            }
            if (fieldFilters != null && !fieldFilters.isEmpty() && !fieldFilters.stream().allMatch(TaskFieldFilter::isEmpty)) {
                List<Long> taskIdsByTaskFilters = mid.getTaskIdsByTaskFilters(fieldFilters);
                predicates.add(root.get("taskId").in(taskIdsByTaskFilters));
            }
            if (searchPhrase != null && !searchPhrase.isBlank()) {
                CriteriaBuilder.In<Long> inCauseTaskId = cb.in(root.get("taskId"));
                mid.getTaskIdsByGlobalSearch(searchPhrase).forEach(inCauseTaskId::value);
                mid.getTaskIdsByAddresses(adrd.getAddressInDBByQuery(searchPhrase)).forEach(inCauseTaskId::value);
                comd.getTaskIdsByGlobalSearch(searchPhrase).forEach(inCauseTaskId::value);
                predicates.add(inCauseTaskId);
            }

            if (exclusionIds != null && !exclusionIds.isEmpty())
                predicates.add(cb.not(root.get("taskId").in(exclusionIds)));
            if (tags != null && !tags.isEmpty())
                predicates.add(root.join("tags", JoinType.INNER).get("taskTagId").in(tags));

            if (author != null && !author.isBlank())
                predicates.add(cb.equal(root.join("creator").get("login"), author));
            if (assignedEmployee != null && !assignedEmployee.isBlank())
                predicates.add(cb.equal(workLogEmployeesJoin.get("login"), assignedEmployee));
            if (dateOfCreation != null && dateOfCreation.start() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("created"), dateOfCreation.start()));
            if (dateOfCreation != null && dateOfCreation.end() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("created"), dateOfCreation.end()));
            if (dateOfClose != null && dateOfClose.start() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("closed"), dateOfClose.start()));
            if (dateOfClose != null && dateOfClose.end() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("closed"), dateOfClose.end()));
            if (actualFrom != null && actualFrom.start() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("actualFrom"), actualFrom.start()));
            if (actualFrom != null && actualFrom.end() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("actualFrom"), actualFrom.end()));
            if (actualTo != null && actualTo.start() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("actualTo"), actualTo.start()));
            if (actualTo != null && actualTo.end() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("actualTo"), actualTo.end()));
            if (schedulingType != null && schedulingType != SchedulingType.ALL)
                switch (schedulingType){
                    case SCHEDULED -> predicates.add(cb.or(cb.isNotNull(root.get("actualFrom")), cb.isNotNull(root.get("actualTo"))));
                    case UNSCHEDULED -> predicates.add(cb.and(cb.isNull(root.get("actualFrom")),  cb.isNull(root.get("actualTo"))));
                    case PLANNED -> predicates.add(cb.isNotNull(root.get("actualFrom")));
                    case DEADLINE -> predicates.add(cb.and(cb.isNull(root.get("actualFrom")), cb.isNotNull(root.get("actualTo"))));
                    case EXCEPT_PLANNED -> predicates.add(cb.isNull(root.get("actualFrom")));
                }

            if (employeeTask != null) {
                predicates.add(cb.or(
                        root.join("employeesObservers", JoinType.LEFT).get("login").in(employeeTask.getLogin()),
                        root.join("departmentsObservers", JoinType.LEFT).join("employees", JoinType.LEFT).get("login").in(employeeTask.getLogin())));
                predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            }

            predicates.add(cb.equal(root.get("deleted"), false));

            return predicates.toArray(Predicate[]::new);
        }

        public enum SchedulingType{
            ALL("ALL"),
            SCHEDULED("SCHEDULED"),
            UNSCHEDULED("UNSCHEDULED"),
            PLANNED("PLANNED"),
            DEADLINE("DEADLINE"),
            EXCEPT_PLANNED("EXCEPT_PLANNED");

            private final String value;

            SchedulingType(String value){
                this.value = value;
            }

            public String getValue(){
                return value;
            }
        }
    }

    @Data
    public static class MovingToDirectoryForm{
        private List<Long> taskIds;
        private Long directoryId;
    }

    public static class UpdateTasksCountWorker{
        private List<AbstractTaskCounterPath> paths = new ArrayList<>();
        public static UpdateTasksCountWorker of(Task task){
            UpdateTasksCountWorker worker = new UpdateTasksCountWorker();
            worker.appendPath(task);
            return worker;
        }

        public UpdateTasksCountWorker appendPath(Task task){
            paths.addAll(task.getListOfCounterPaths());
            return this;
        }

        public void execute(TaskDispatcher context){
            for (AbstractTaskCounterPath path : paths) {
                context.stompController.updateTaskCounter(context.getTasksCount(path), path);
            }
            context.stompController.movedTask();
        }
    }
}
