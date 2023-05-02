package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.modules.transport.IDuration;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.dto.mapper.TaskMapper;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
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
import org.javatuples.Triplet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Component
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

    public TaskDispatcher(TaskRepository taskRepository, ModelItemDispatcher modelItemDispatcher, WireframeDispatcher wireframeDispatcher, CommentDispatcher commentDispatcher, TaskStageRepository taskStageRepository, DepartmentDispatcher departmentDispatcher, EmployeeDispatcher employeeDispatcher, WorkLogDispatcher workLogDispatcher, TaskTagDispatcher taskTagDispatcher, AddressDispatcher addressDispatcher) {
        this.taskRepository = taskRepository;
        this.modelItemDispatcher = modelItemDispatcher;
        this.wireframeDispatcher = wireframeDispatcher;
        this.commentDispatcher = commentDispatcher;
        this.taskStageRepository = taskStageRepository;
        this.departmentDispatcher = departmentDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.taskTagDispatcher = taskTagDispatcher;
    }

    public Task createTask(Task sourceTask, Employee employee) throws IllegalFields {
        // Создаем временный объект задачи
        Task createdTask = new Task();
        // Устанавливаем время создания задачи
        createdTask.setCreated(Timestamp.from(Instant.now()));
        // Устанавливаем время обновления задачи
        createdTask.setUpdated(Timestamp.from(Instant.now()));
        // Устанавливаем автора задачи
        createdTask.setCreator(employee);

        // Получаем список наблюдателей задачи по-умолчанию из шаблона
        List<DefaultObserver> defaultObservers = sourceTask.getModelWireframe().getDefaultObservers();
        // Выбираем из базы данных действующих наблюдателей задачи
        List<Employee> employeesObservers = employeeDispatcher.getByIdSet(DefaultObserver.getSetOfEmployees(defaultObservers).stream().map(Employee::getLogin).collect(Collectors.toSet()));
        List<Department> departmentsObservers = departmentDispatcher.getByIdSet(DefaultObserver.getSetOfDepartments(defaultObservers).stream().map(Department::getDepartmentId).collect(Collectors.toSet()));

        // Устанавливаем сотрудников как наблюдателей задачи
        createdTask.setEmployeesObservers(employeesObservers);
        // Устанавливаем отделы как наблюдателей задачи
        createdTask.setDepartmentsObservers(departmentsObservers);

        //Проверяем есть ли установленный шаблон в запросе
        if (sourceTask.getModelWireframe() == null) throw new IllegalFields("Не установлен шаблон для создания задачи");

        // Получаем шаблон задачи из бд по идентификатору и устанавливаем его в createdTask
        Wireframe wireframe = wireframeDispatcher.getWireframeById(sourceTask.getModelWireframe().getWireframeId(), false);
        // Если wireframe null то выбрасываем исключение
        if (wireframe == null) throw new IllegalFields("В базе данных не найден шаблон для создания задачи");

        createdTask.setCurrentStage(wireframe.getFirstStage());

        // Устанавливаем статус задачи как активная
        createdTask.setTaskStatus(TaskStatus.ACTIVE);
        // Устанавливаем шаблон задачи
        createdTask.setModelWireframe(wireframe);

        //Подготавливаем данные в задаче для сохранения
        List<ModelItem> modelItems = modelItemDispatcher.prepareModelItems(ModelItemDispatcher.cleanToCreate(sourceTask.getFields()));

        // Устанавливаем поля задачи
        createdTask.setFields(modelItems);

        // Получаем все дочерние задачи из бд по идентификатору и устанавливаем их в createdTask
        if (sourceTask.getChildren() != null && sourceTask.getChildren().size() > 0) {

            List<Task> childrenFromDB = taskRepository.findAllById(sourceTask.getChildren().stream().map(Task::getTaskId).collect(Collectors.toList()));

            Task savedTask = taskRepository.save(createdTask);
            savedTask.setChildren(childrenFromDB);
            return taskRepository.save(savedTask);
        }

        // Получаем родительскую задачу из бд по идентификатору и устанавливаем её в createdTask
        if (sourceTask.getParent() != null) {

            Task parentFromDB = taskRepository.findById(sourceTask.getParent()).orElse(null);

            if (parentFromDB != null) {
                createdTask.setParent(parentFromDB.getTaskId());
                parentFromDB.getChildren().add(createdTask);
                Task saved = taskRepository.save(createdTask);
                taskRepository.save(parentFromDB);
                return saved;
            }
        }

        return taskRepository.save(createdTask);
    }

    @Transactional(readOnly = true)
    public Page<Task> getTasks(Integer page, Integer limit, @Nullable List<TaskStatus> status, @Nullable Set<Long> template,
                               @Nullable List<FilterModelItem> filters, @Nullable String commonFilteringString, @Nullable String taskCreator,
                               @Nullable DateRange creationRange, @Nullable Set<Long> filterTags, @Nullable Set<Long> exclusionIds, @Nullable Employee employeeTask) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isEmpty()) predicates.add(root.get("taskStatus").in(status));
            if (template != null && !template.isEmpty())
                predicates.add(root.join("modelWireframe").get("wireframeId").in(template));
            if (filters != null && !filters.isEmpty())
                predicates.add(root.get("taskId").in(modelItemDispatcher.getTaskIdsByFilters(filters)));

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

            if(employeeTask != null) {
                predicates.add(cb.or(
                        root.join("employeesObservers", JoinType.LEFT).get("login").in(employeeTask.getLogin()),
                        root.join("departmentsObservers", JoinType.LEFT).join("employees").get("login").in(employeeTask.getLogin())));
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

    public Task unsafeSave(TaskDto task) {
        return taskRepository.save(TaskMapper.fromDto(task));
    }

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

    public Pair<Task, WorkLog> assignInstallers(Long taskId, Set<Employee> installers, Employee creator) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        WorkLog existed = workLogDispatcher.getActiveWorkLogByTask(task).orElse(null);
        if (existed != null) throw new IllegalFields("Задаче уже назначены другие монтажники");
        WorkLog workLog = workLogDispatcher.createWorkLog(task, installers, creator);
        task.setTaskStatus(TaskStatus.PROCESSING);
        task.setUpdated(Timestamp.from(Instant.now()));
        return Pair.of(taskRepository.save(task), workLog);
    }

    public Pair<Task, WorkLog> forceCloseWorkLog(Long taskId, Employee employeeFromRequest) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        WorkLog workLog = workLogDispatcher.getActiveWorkLogByTask(task).orElse(null);

        if (workLog == null) throw new EntryNotFound("Не найдено ни одного журнала работ для принудительного закрытия");

        workLog.setIsForceClosed(true);
        workLog.setClosed(Timestamp.from(Instant.now()));

        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setUpdated(Timestamp.from(Instant.now()));

        return Pair.of(taskRepository.save(task), workLogDispatcher.save(workLog));
    }

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

    public Task modifyTags(Long taskId, Set<TaskTag> tags) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        // Check if tags are valid
        if (!taskTagDispatcher.valid(tags)) throw new EntryNotFound("Часть тегов не найдена в базе данных");
        task.setTags(tags);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

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

    public Task unbindTaskResponsible(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setResponsible(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Task changeTaskActualFrom(Long id, Instant datetime) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        if (task.getActualTo() != null && datetime.isAfter(task.getActualTo().toInstant())) task.setActualTo(null);
        task.setActualFrom(Timestamp.from(datetime));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Task changeTaskActualTo(Long id, Instant datetime) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        if (task.getActualFrom() != null && datetime.isBefore(task.getActualFrom().toInstant()))
            task.setActualFrom(null);
        task.setActualTo(Timestamp.from(datetime));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Task clearTaskActualFrom(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setActualFrom(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Task clearTaskActualTo(Long id) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        task.setActualTo(null);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Task close(Long id) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(id).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + id);
        if (task.getTaskStatus().equals(TaskStatus.CLOSE)) throw new IllegalFields("Задача уже закрыта");
        if (task.getTaskStatus().equals(TaskStatus.PROCESSING))
            throw new IllegalFields("Пока задача отдана монтажникам её нельзя закрыть");
        task.setTaskStatus(TaskStatus.CLOSE);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Task reopen(Long taskId) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if (task.getTaskStatus().equals(TaskStatus.ACTIVE)) throw new IllegalFields("Задача уже активна");
        if (task.getTaskStatus().equals(TaskStatus.PROCESSING))
            throw new IllegalFields("Невозможно активировать задачу");
        task.setTaskStatus(TaskStatus.ACTIVE);
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }


    public Task edit(Long taskId, List<ModelItem> modelItems) throws EntryNotFound, IllegalFields {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if (task.getTaskStatus().equals(TaskStatus.CLOSE)) throw new IllegalFields("Задача уже закрыта");

        // Редактируем поля задачи и сохраняем их в БД
        task.editFields(modelItemDispatcher.prepareModelItems(modelItems));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Page<Task> getIncomingTasks(Integer page, Integer limit, Employee employee, Set<Long> template) {
        return taskRepository.findAll((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            if (template != null && !template.isEmpty())
                predicates.add(root.join("modelWireframe").get("wireframeId").in(template));

            predicates.add(cb.or(root.join("employeesObservers", JoinType.LEFT).get("login").in(employee.getLogin()), root.join("departmentsObservers", JoinType.LEFT).join("employees").get("login").in(employee.getLogin())));

            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);

            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, limit, Sort.by(Sort.Order.desc("updated").nullsLast())));
    }

    public Long getIncomingTasksCount(Employee employee) {
        return taskRepository.count((root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(root.join("employeesObservers", JoinType.LEFT).get("login").in(employee.getLogin()), root.join("departmentsObservers", JoinType.LEFT).join("employees").get("login").in(employee.getLogin())));

            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            predicates.add(cb.equal(root.get("deleted"), false));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
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
                            root.join("departmentsObservers", JoinType.LEFT).join("employees").get("login").in(whose.getLogin())
                    )
            );

            predicates.add(cb.equal(root.get("deleted"), false));
            predicates.add(cb.notEqual(root.get("taskStatus"), TaskStatus.CLOSE));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public Task moveTaskScheduled(Long taskId, IDuration delta) throws EntryNotFound {
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) throw new EntryNotFound("Не найдена задача с идентификатором " + taskId);
        if(task.getActualFrom() != null) task.setActualFrom(delta.shift(task.getActualFrom()));
        if(task.getActualTo() != null) task.setActualTo(delta.shift(task.getActualTo()));
        task.setUpdated(Timestamp.from(Instant.now()));
        return taskRepository.save(task);
    }

    public Long getCountByWireframe(Wireframe wireframe) {
        return taskRepository.countByModelWireframe(wireframe);
    }
}
