package com.microel.trackerbackend.services.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.modules.exception.DateRangeReadException;
import com.microel.trackerbackend.modules.transport.ChangeTaskObserversDTO;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.modules.transport.IDuration;
import com.microel.trackerbackend.parsers.addresses.AddressParser;
import com.microel.trackerbackend.parsers.oldtracker.AddressCorrectingPool;
import com.microel.trackerbackend.parsers.oldtracker.OldTracker;
import com.microel.trackerbackend.parsers.oldtracker.OldTrackerParserSettings;
import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.dto.mapper.TaskMapper;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.chat.MessageData;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.TaskJournalItem;
import com.microel.trackerbackend.storage.entities.comments.dto.CommentData;
import com.microel.trackerbackend.storage.entities.comments.dto.CommentDto;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskFieldsSnapshot;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.Observer;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.*;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.exceptions.*;
import com.microel.trackerbackend.storage.repositories.AttachmentRepository;
import org.javatuples.Triplet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("api/private")
public class PrivateRequestController {

    private final WireframeDispatcher wireframeDispatcher;
    private final TaskDispatcher taskDispatcher;
    private final StreetDispatcher streetDispatcher;
    private final CityDispatcher cityDispatcher;
    private final CommentDispatcher commentDispatcher;
    private final ModelItemDispatcher modelItemDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final AttachmentDispatcher attachmentDispatcher;
    private final DepartmentDispatcher departmentsDispatcher;
    private final PositionDispatcher positionDispatcher;
    private final AuthorizationProvider authorizationProvider;
    private final AttachmentRepository attachmentRepository;
    private final TaskEventDispatcher taskEventDispatcher;
    private final StompController stompController;
    private final TaskTagDispatcher taskTagDispatcher;
    private final TaskFieldsSnapshotDispatcher taskFieldsSnapshotDispatcher;
    private final NotificationDispatcher notificationDispatcher;
    private final WorkLogDispatcher workLogDispatcher;
    private final ChatDispatcher chatDispatcher;
    private final TelegramController telegramController;
    private final OldTracker oldTracker;
    private final AddressParser addressParser;
    private final AddressDispatcher addressDispatcher;

    public PrivateRequestController(WireframeDispatcher wireframeDispatcher, TaskDispatcher taskDispatcher,
                                    StreetDispatcher streetDispatcher, CityDispatcher cityDispatcher,
                                    CommentDispatcher commentDispatcher, ModelItemDispatcher modelItemDispatcher,
                                    EmployeeDispatcher employeeDispatcher, AttachmentDispatcher attachmentDispatcher,
                                    DepartmentDispatcher departmentsDispatcher, PositionDispatcher positionDispatcher,
                                    AuthorizationProvider authorizationProvider, AttachmentRepository attachmentRepository,
                                    TaskEventDispatcher taskEventDispatcher, StompController stompController,
                                    TaskTagDispatcher taskTagDispatcher, TaskFieldsSnapshotDispatcher taskFieldsSnapshotDispatcher,
                                    NotificationDispatcher notificationDispatcher, WorkLogDispatcher workLogDispatcher,
                                    ChatDispatcher chatDispatcher, TelegramController telegramController,
                                    OldTracker oldTracker, AddressParser addressParser, AddressDispatcher addressDispatcher) {
        this.wireframeDispatcher = wireframeDispatcher;
        this.taskDispatcher = taskDispatcher;
        this.streetDispatcher = streetDispatcher;
        this.cityDispatcher = cityDispatcher;
        this.commentDispatcher = commentDispatcher;
        this.modelItemDispatcher = modelItemDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.attachmentDispatcher = attachmentDispatcher;
        this.departmentsDispatcher = departmentsDispatcher;
        this.positionDispatcher = positionDispatcher;
        this.authorizationProvider = authorizationProvider;
        this.attachmentRepository = attachmentRepository;
        this.taskEventDispatcher = taskEventDispatcher;
        this.stompController = stompController;
        this.taskTagDispatcher = taskTagDispatcher;
        this.taskFieldsSnapshotDispatcher = taskFieldsSnapshotDispatcher;
        this.notificationDispatcher = notificationDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.chatDispatcher = chatDispatcher;
        this.telegramController = telegramController;
        this.oldTracker = oldTracker;
        this.addressParser = addressParser;
        this.addressDispatcher = addressDispatcher;
    }

    // Создание шаблона задачи
    @PostMapping("wireframe")
    public ResponseEntity<Wireframe> createWireframe(@RequestBody Wireframe body, HttpServletRequest request) {
        return ResponseEntity.ok(
                wireframeDispatcher.createWireframe(body, getEmployeeFromRequest(request))
        );
    }

    // Получение списка шаблонов
    @GetMapping("wireframes")
    public ResponseEntity<List<Wireframe>> getWireframes() {
        return ResponseEntity.ok(wireframeDispatcher.getAllWireframes(true));
    }

    // Получение списка шаблонов
    @GetMapping("wireframes/names")
    public ResponseEntity<List<Wireframe>> getWireframesNames() {
        return ResponseEntity.ok(wireframeDispatcher.getAllWireframes(false).stream().map(Wireframe::toDropdownList).collect(Collectors.toList()));
    }

    // Получение шаблона по идентификатору
    @GetMapping("wireframe/{id}")
    public ResponseEntity<Wireframe> getWireframe(@PathVariable Long id) {
        Wireframe wireframe = wireframeDispatcher.getWireframeById(id);
        if (wireframe == null) throw new ResponseException("Шаблон не найден");
        return ResponseEntity.ok(wireframe);
    }

    // Получение списка полей шаблона
    @GetMapping("wireframe/{id}/fields")
    public ResponseEntity<List<FieldItem>> getWireframeFields(@PathVariable Long id) {
        Wireframe wireframe = wireframeDispatcher.getWireframeById(id);
        if (wireframe == null) throw new ResponseException("Шаблон не найден");
        List<FieldItem> fields = new ArrayList<FieldItem>();
        wireframe.getSteps().forEach(step -> {
            fields.addAll(step.getFields());
        });
        return ResponseEntity.ok(fields);
    }

    // Редактирование шаблона задачи
    @PatchMapping("wireframe")
    public ResponseEntity<Object> updateWireframe(@RequestBody Wireframe body) {
        return ResponseEntity.ok(wireframeDispatcher.updateWireframe(body));
    }

    // Удаление шаблона задачи
    @DeleteMapping("wireframe/{id}")
    public ResponseEntity<Void> deleteWireframe(@PathVariable Long id) {
        try {
            wireframeDispatcher.deleteWireframe(id);
            return ResponseEntity.ok().build();
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Создание новых улиц
//    @PostMapping("streets/{cityId}")
//    public ResponseEntity<Void> appendAllStreets(@RequestBody List<String> body, @PathVariable Long cityId) {
//        try {
//            streetDispatcher.saveAll(body, cityId);
//            return ResponseEntity.ok().build();
//        } catch (EntryNotFound e) {
//            throw new ResponseException(e.getMessage());
//        }
//    }

    // Получение всех городов
    @GetMapping("cities")
    public ResponseEntity<List<City>> getAllCities() {
        return ResponseEntity.ok(cityDispatcher.getCities());
    }

    // Получение всех улиц в населенном пункте
    @GetMapping("streets/{cityId}")
    public ResponseEntity<List<Street>> getAllStreets(@PathVariable Long cityId) {
        return ResponseEntity.ok(streetDispatcher.getStreetsInCity(cityId));
    }

    // Создание новой задачи
    @PostMapping("task")
    public ResponseEntity<Task> createTask(@RequestBody Task body, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);

        try {
            // Создаём задачу в базе данных
            Task createdTask = taskDispatcher.createTask(body, getEmployeeFromRequest(request));

            Set<Employee> observers = createdTask.getAllEmployeesObservers(employee);
            // Создаем оповещение о новой задаче
            notificationDispatcher.createNotification(observers, Notification.taskCreated(createdTask));

            // Отправляем сигнал пользователям, что задача создана
            stompController.createTask(createdTask);

            // Если в задаче при создании были дочерние задачи, обновляем информацию в них
            if (createdTask.getChildren() != null)
                createdTask.getChildren().forEach(child -> {
                    stompController.updateTask(child);

                    TaskEvent taskChildEvent = taskEventDispatcher.appendEvent(
                            TaskEvent.linkedToChildTask(createdTask, Set.of(child), employee)
                    );
                    stompController.createTaskEvent(createdTask.getTaskId(), taskChildEvent);

                    TaskEvent taskParentEvent = taskEventDispatcher.appendEvent(
                            TaskEvent.linkedToParentTask(child, createdTask, employee)
                    );
                    stompController.createTaskEvent(child.getTaskId(), taskParentEvent);
                });

            // Если задача создавалась как дочерняя, то обновляем информацию в родительской задаче
            if (createdTask.getParent() != null) {
                try {
                    Task parentTask = taskDispatcher.getTask(createdTask.getParent());
                    stompController.updateTask(parentTask);

                    TaskEvent taskParentEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToParentTask(createdTask, parentTask, employee));
                    stompController.createTaskEvent(createdTask.getTaskId(), taskParentEvent);

                    TaskEvent taskTargetEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToChildTask(parentTask, Set.of(createdTask), employee));
                    stompController.createTaskEvent(parentTask.getTaskId(), taskTargetEvent);

                } catch (EntryNotFound e) {
                    throw new ResponseException("Не удалось получить родительскую задачу");
                }
            }

            return ResponseEntity.ok(createdTask);
        } catch (IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаление задачи
    @DeleteMapping("task/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task task = taskDispatcher.deleteTask(id);
            Set<Employee> observers = task.getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.taskDeleted(task, employeeFromRequest));
        } catch (EntryNotFound e) {
            throw new ResponseException("Задача с идентификатором " + id + " не найдена в базе данных");
        }
        return ResponseEntity.ok().build();
    }

    // Получение задачи по идентификатору
    @GetMapping("task/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(taskDispatcher.getTask(id));
        } catch (EntryNotFound e) {
            throw new ResponseException("Задача c id:" + id + " не найдена");
        }
    }

    // Получение коренного родителя задачи по идентификатору
    @GetMapping("task/{id}/root")
    public ResponseEntity<Task> getRootTask(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(taskDispatcher.getRootTask(id));
        } catch (EntryNotFound e) {
            throw new ResponseException("Задача c id:" + id + " не найдена");
        }
    }

    // Получение полей задачи по идентификатору
    @GetMapping("task/{id}/fields")
    public ResponseEntity<List<ModelItem>> getTaskFields(@PathVariable Long id) {
        return ResponseEntity.ok(modelItemDispatcher.getFieldsTask(id));
    }

    // Получение страницу с задачами используя фильтрацию
    @GetMapping("tasks")
    public ResponseEntity<Page<TaskDto>> getTasks(@RequestParam Integer page, @RequestParam Integer limit,
                                                  @RequestParam @Nullable List<TaskStatus> status, @RequestParam @Nullable Set<Long> template,
                                                  @RequestParam @Nullable String templateFilter, @RequestParam @Nullable String globalContext,
                                                  @RequestParam @Nullable String author, @RequestParam @Nullable String dateOfCreation,
                                                  @RequestParam @Nullable Set<Long> exclusionIds, @RequestParam @Nullable Set<Long> tags,
                                                  @RequestParam @Nullable Boolean onlyMy, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        List<FilterModelItem> filtersList = null;
        try {
            filtersList = new ObjectMapper().readValue(templateFilter, new TypeReference<>() {
            });
            filtersList = filtersList.stream().filter(filter -> filter.getValue() != null).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new ResponseException(e.getMessage());
        } catch (IllegalArgumentException ignore) {
        }

        DateRange creationRange = null;
        try {
            creationRange = DateRange.from(dateOfCreation);
        } catch (DateRangeReadException e) {
            throw new ResponseException("Не удалось прочитать диапазон времени");
        }

        Instant startDBRequest = Instant.now();

        Page<Task> tasks = null;
        if (onlyMy != null && onlyMy) {
            tasks = taskDispatcher.getTasks(page, limit, null, template, filtersList,
                    globalContext, author, creationRange, tags, exclusionIds, employee);
        } else {
            tasks = taskDispatcher.getTasks(page, limit, status, template, filtersList,
                    globalContext, author, creationRange, tags, exclusionIds, null);
        }

        Instant endDBRequest = Instant.now();
        Duration dbDuration = Duration.between(startDBRequest, endDBRequest);

        return ResponseEntity.ok()
                .header("Server-Timing", "db;desc=\"DB Reading\";dur=" + dbDuration.toMillis())
                .body(tasks.map(TaskMapper::toListObject));
    }

    // Получает страницу с задачами принадлежащими текущему наблюдателю
    @GetMapping("tasks/incoming")
    public ResponseEntity<Page<Task>> getIncomingTasks(@RequestParam Integer page, @RequestParam Integer limit,
                                                       @RequestParam @Nullable Set<Long> template, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasks(page, limit, employee, template));
    }

    // Получает страницу с задачами принадлежащими текущему наблюдателю
    @GetMapping("tasks/incoming/count")
    public ResponseEntity<Long> getIncomingTasksCount(HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCount(employee));
    }

    // Получает список с задачами запланированных на определенный период
    @GetMapping("tasks/scheduled")
    public ResponseEntity<List<Task>> getScheduledTasks(@RequestParam Timestamp start, @RequestParam Timestamp end,
                                                        HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getScheduledTasks(employee, start, end));
    }

    // Сдвигает даты актуальности на определенный промежуток времени
    @PatchMapping("task/{taskId}/scheduled/move")
    public ResponseEntity<Task> moveTaskScheduled(@PathVariable Long taskId, @RequestBody IDuration delta, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.moveTaskScheduled(taskId, delta);
            stompController.updateTask(task);
            TaskEvent taskEvent = null;
            // Создаем события в журнале задачи если даты изменились
            if (task.getActualFrom() != null) {
                taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeActualFrom(task, task.getActualFrom().toInstant(), employee));
                stompController.createTaskEvent(taskId, taskEvent);
            }
            if (task.getActualTo() != null) {
                taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeActualTo(task, task.getActualTo().toInstant(), employee));
                stompController.createTaskEvent(taskId, taskEvent);
            }
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }


    // Получение страницы с задачами которые не находятся ни в одной стадии
    @GetMapping("tasks/template/{templateId}/stage")
    public ResponseEntity<Page<Task>> getActiveTasksByNullStage(@PathVariable Long templateId, @RequestParam Long offset, @RequestParam Integer limit) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTasksByStage(templateId, null, offset, limit));
    }

    // Получение страницы с задачами из конкретной стадии
    @GetMapping("tasks/template/{templateId}/stage/{stageId}")
    public ResponseEntity<Page<Task>> getActiveTasksByStage(@PathVariable Long templateId, @PathVariable String stageId, @RequestParam Long offset, @RequestParam Integer limit) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTasksByStage(templateId, stageId, offset, limit));
    }

    // Получение списка идентификаторов задач без стадии
    @GetMapping("tasks/template/{templateId}/stage/taskIdOnly")
    public ResponseEntity<List<Long>> getActiveTaskIdsByNullStage(@PathVariable Long templateId) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTaskIdsByStage(templateId, null));
    }

    // Получение списка идентификаторов задач из конкретной стадии
    @GetMapping("tasks/template/{templateId}/stage/{stageId}/taskIdOnly")
    public ResponseEntity<List<Long>> getActiveTaskIdsByStage(@PathVariable Long templateId, @PathVariable String stageId) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTaskIdsByStage(templateId, stageId));
    }

    // Изменение стадии задачи
    @PatchMapping("task/{taskId}/stage")
    public ResponseEntity<Task> changeTaskStage(@PathVariable Long taskId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task task = taskDispatcher.changeTaskStage(taskId, body.get("stageId"));
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.taskStageChanged(task, employeeFromRequest));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeStage(task, task.getCurrentStage(), employeeFromRequest));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Отсоединяет задачу от родительской
    @PatchMapping("task/{taskId}/unlink-parent")
    public ResponseEntity<Task> unlinkParentTask(@PathVariable Long taskId, HttpServletRequest request) {
        try {
            Pair<Task, Task> taskTaskPair = taskDispatcher.unlinkFromParent(taskId);
            stompController.updateTask(taskTaskPair.getFirst());
            stompController.updateTask(taskTaskPair.getSecond());
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkFromParentTask(taskTaskPair.getFirst(), taskTaskPair.getSecond(), getEmployeeFromRequest(request)));
            TaskEvent parentTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkChildTask(taskTaskPair.getSecond(), Set.of(taskTaskPair.getFirst()), getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);
            stompController.createTaskEvent(taskTaskPair.getSecond().getTaskId(), parentTaskEvent);
            return ResponseEntity.ok(taskTaskPair.getFirst());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Устанавливает родительскую задачу
    @PatchMapping("task/{taskId}/link-to-parent")
    public ResponseEntity<Task> changeLinkToParentTask(@PathVariable Long taskId, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        try {
            Triplet<Task, Task, Task> taskTriplet = taskDispatcher.changeLinkToParentTask(taskId, body.get("parentTaskId"));
            Employee author = getEmployeeFromRequest(request);
            stompController.updateTask(taskTriplet.getValue0());
            TaskEvent targetTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToParentTask(taskTriplet.getValue0(), taskTriplet.getValue1(), author));
            stompController.createTaskEvent(taskId, targetTaskEvent);
            stompController.updateTask(taskTriplet.getValue1());
            TaskEvent parentTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToChildTask(taskTriplet.getValue1(), Set.of(taskTriplet.getValue0()), author));
            stompController.createTaskEvent(taskTriplet.getValue1().getTaskId(), parentTaskEvent);
            if (taskTriplet.getValue2() != null) {
                stompController.updateTask(taskTriplet.getValue2());
                TaskEvent previousParentTasksEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkChildTask(taskTriplet.getValue2(), Set.of(taskTriplet.getValue0()), author));
                stompController.createTaskEvent(taskTriplet.getValue2().getTaskId(), previousParentTasksEvent);
            }
            return ResponseEntity.ok(taskTriplet.getValue0());
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Добавляет в задачу ссылки на дочерние задачи
    @PatchMapping("task/{taskId}/append-links-to-children")
    public ResponseEntity<Task> appendLinksToChildrenTask(@PathVariable Long taskId, @RequestBody Set<Long> childIds, HttpServletRequest request) {
        try {
            Triplet<Task, List<Task>, List<Pair<Task, Task>>> complexOfTasks = taskDispatcher.appendLinksToChildrenTask(taskId, childIds);
            Employee author = getEmployeeFromRequest(request);
            stompController.updateTask(complexOfTasks.getValue0());
            TaskEvent targetTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToChildTask(complexOfTasks.getValue0(), new HashSet<>(complexOfTasks.getValue1()), author));
            stompController.createTaskEvent(taskId, targetTaskEvent);
            complexOfTasks.getValue1().forEach(childTasks -> {
                stompController.updateTask(childTasks);
                TaskEvent childTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToParentTask(childTasks, complexOfTasks.getValue0(), author));
                stompController.createTaskEvent(childTasks.getTaskId(), childTaskEvent);
            });
            complexOfTasks.getValue2().forEach(previousParentChild -> {
                stompController.updateTask(previousParentChild.getFirst());
                TaskEvent previousParentTasksEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkChildTask(previousParentChild.getFirst(), Set.of(previousParentChild.getSecond()), author));
                stompController.createTaskEvent(previousParentChild.getFirst().getTaskId(), previousParentTasksEvent);
            });
            return ResponseEntity.ok(complexOfTasks.getValue0());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Устанавливает теги задачи
    @PatchMapping("task/{taskId}/tags")
    public ResponseEntity<Task> changeTaskTags(@PathVariable Long taskId, @RequestBody Set<TaskTag> body, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.modifyTags(taskId, body);
            stompController.updateTask(task);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeTags(task, body, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Создает тег задачи
    @PostMapping("task-tag")
    public ResponseEntity<TaskTag> createTaskTag(@RequestBody TaskTag body, HttpServletRequest request) {
        try {
            TaskTag taskTag = taskTagDispatcher.create(body, getEmployeeFromRequest(request));
            stompController.createTaskTag(taskTag);
            return ResponseEntity.ok(taskTag);
        } catch (AlreadyExists e) {
            throw new ResponseException("Тег с таким именем уже существует");
        } catch (IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет тег задачи
    @PatchMapping("task-tag")
    public ResponseEntity<TaskTag> updateTaskTag(@RequestBody TaskTag body, HttpServletRequest request) {
        try {
            TaskTag modifyTag = taskTagDispatcher.modify(body);
            stompController.updateTaskTag(modifyTag);
            return ResponseEntity.ok(modifyTag);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет тег задачи
    @DeleteMapping("task-tag/{id}")
    public ResponseEntity<Void> deleteTaskTag(@PathVariable Long id, HttpServletRequest request) {
        try {
            stompController.deleteTaskTag(taskTagDispatcher.delete(id));
            return ResponseEntity.ok().build();
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает список доступных тегов задачи
    @GetMapping("task-tags")
    public ResponseEntity<List<TaskTag>> getAllTaskTags(@RequestParam @Nullable Boolean includingRemote, HttpServletRequest request) {
        return ResponseEntity.ok(taskTagDispatcher.getAll(includingRemote));
    }

    // Создает комментарий к задаче
    @PostMapping("comment")
    public ResponseEntity<Comment> createComment(@RequestBody CommentData body, HttpServletRequest request) {
        if (body.getTaskId() == null) throw new ResponseException("Идентификатор задачи не может быть пустым");
        Employee currentUser = getEmployeeFromRequest(request);
        try {
            Comment comment = commentDispatcher.create(body, currentUser);
            Set<Employee> taskObservers = comment.getParent().getAllEmployeesObservers(currentUser);
            notificationDispatcher.createNotification(taskObservers, Notification.newComment(comment));
            stompController.createComment(comment);
            return ResponseEntity.ok(comment);
        } catch (EmptyFile e) {
            throw new ResponseException("Нельзя сохранить пустой файл");
        } catch (WriteError e) {
            throw new ResponseException("Ошибка записи");
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет комментарий к задаче
    @PatchMapping("comment")
    public ResponseEntity<Comment> updateComment(@RequestBody Comment body, HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        try {
            Comment comment = commentDispatcher.update(body, currentUser);
            stompController.updateComment(comment);
            return ResponseEntity.ok(comment);
        } catch (EntryNotFound | NotOwner | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет комментарий к задаче
    @DeleteMapping("comment/{commentId}")
    public ResponseEntity<Comment> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        try {
            Comment comment = commentDispatcher.delete(commentId, currentUser);
            stompController.deleteComment(comment);
            return ResponseEntity.ok(comment);
        } catch (EntryNotFound | NotOwner e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает страницу с комментариями и событиями в конкретной задаче
    @GetMapping("task/{id}/journal")
    public ResponseEntity<Page<TaskJournalItem>> getTaskJournal(@PathVariable Long id, @RequestParam Long offset, @RequestParam Integer limit) {
        Page<CommentDto> comments = commentDispatcher.getComments(id, offset, limit);
        List<TaskJournalItem> commentItems = comments.getContent().stream().map(commentDto -> (TaskJournalItem) commentDto).collect(Collectors.toList());
        if (comments.getTotalElements() == 0L) {
            List<TaskEvent> taskEvents = taskEventDispatcher.getTaskEvents(id);
            // Concat events and comments
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).collect(Collectors.toList());
            commentItems.addAll(taskEventsItems);
            commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
        } else if (comments.isLast()) {
            CommentDto firstComment = comments.getContent().get(0);
            List<TaskEvent> taskEvents = new ArrayList<>();
            if (offset == 0) {
                taskEvents = taskEventDispatcher.getTaskEvents(id);
            } else if (offset > 0) {
                taskEvents = taskEventDispatcher.getTaskEventsFrom(id, firstComment.getCreated());
            }
            // Concat events and comments and sort by creation date
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).collect(Collectors.toList());
            commentItems.addAll(taskEventsItems);
            commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
        } else {
            CommentDto firstComment = comments.getContent().get(0);
            CommentDto lastComment = comments.getContent().get(comments.getContent().size() - 1);
            List<TaskEvent> taskEvents = new ArrayList<>();
            if (offset == 0) {
                taskEvents = taskEventDispatcher.getTaskEventsTo(id, lastComment.getCreated());
            } else if (offset > 0) {
                taskEvents = taskEventDispatcher.getTaskEvents(id, firstComment.getCreated(), lastComment.getCreated());
            }
            // Concat events and comments and sort by creation date
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).collect(Collectors.toList());
            commentItems.addAll(taskEventsItems);
            commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
        }

        return ResponseEntity.ok(new PageImpl<>(commentItems, Pageable.unpaged(), comments.getTotalElements()));
    }

    // Изменяет наблюдателей в задаче
    @PatchMapping("task/{id}/observers")
    public ResponseEntity<Task> changeTaskObservers(@PathVariable Long id, @RequestBody ChangeTaskObserversDTO body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task previousTask = taskDispatcher.getTask(id);
            Set<Employee> previousObservers = new HashSet<>(previousTask.getAllEmployeesObservers(employeeFromRequest));

            Task task = taskDispatcher.changeTaskObservers(id, body.getDepartmentObservers(), body.getPersonalObservers());
            Set<Employee> newObservers = new HashSet<>(task.getAllEmployeesObservers(employeeFromRequest));

            // Получаем Set новых наблюдателей из разницы newObservers и previousObserver
            Set<Employee> employeesObservers = new HashSet<>(newObservers);
            employeesObservers.removeAll(previousObservers);

            notificationDispatcher.createNotification(employeesObservers, Notification.youObserver(task));

            stompController.updateTask(task);

            List<Observer> observers = new ArrayList<>();
            observers.addAll(task.getEmployeesObservers());
            observers.addAll(task.getDepartmentsObservers());
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeObservers(task, observers, employeeFromRequest));

            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет ответственного в задаче
    @PatchMapping("task/{id}/responsible")
    public ResponseEntity<Task> changeTaskResponsible(@PathVariable Long id, @RequestBody Employee body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task task = taskDispatcher.changeTaskResponsible(id, body);
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.youResponsible(task));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeResponsible(task, body, employeeFromRequest));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет ответственного в задаче
    @PatchMapping("task/{id}/unbind-responsible")
    public ResponseEntity<Task> unbindTaskResponsible(@PathVariable Long id, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.unbindTaskResponsible(id);
            stompController.updateTask(task);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.unbindResponsible(task, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет дату начала выполнения задачи
    @PatchMapping("task/{id}/actual-from")
    public ResponseEntity<Task> changeTaskActualFrom(@PathVariable Long id, @RequestBody Instant body, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.changeTaskActualFrom(id, body);
            stompController.updateTask(task);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeActualFrom(task, body, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет дату окончания выполнения задачи
    @PatchMapping("task/{id}/actual-to")
    public ResponseEntity<Task> changeTaskActualTo(@PathVariable Long id, @RequestBody Instant body, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.changeTaskActualTo(id, body);
            stompController.updateTask(task);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeActualTo(task, body, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет дату начала выполнения задачи
    @PatchMapping("task/{id}/clear-actual-from-date")
    public ResponseEntity<Task> clearTaskActualFromDate(@PathVariable Long id, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.clearTaskActualFrom(id);
            stompController.updateTask(task);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.clearActualFrom(task, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет дату окончания выполнения задачи
    @PatchMapping("task/{id}/clear-actual-to-date")
    public ResponseEntity<Task> clearTaskActualToDate(@PathVariable Long id, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.clearTaskActualTo(id);
            stompController.updateTask(task);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.clearActualTo(task, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает страницу комментариев к задаче
    @GetMapping("comments")
    public ResponseEntity<Page<CommentDto>> getComments(@RequestParam Long taskId, @RequestParam Long offset, @RequestParam Integer limit) {
        return ResponseEntity.ok(commentDispatcher.getComments(taskId, offset, limit));
    }

    // Получает список сотрудников
    @GetMapping("employees/list")
    public ResponseEntity<List<Employee>> getEmployeesList(@RequestParam @Nullable String globalFilter, @RequestParam @Nullable Boolean showDeleted, @RequestParam @Nullable Boolean showOffsite) {
        if (globalFilter != null && !globalFilter.isBlank() || showDeleted != null || showOffsite != null)
            return ResponseEntity.ok(employeeDispatcher.getEmployeesList(globalFilter, showDeleted, showOffsite));
        return ResponseEntity.ok(employeeDispatcher.getEmployeesList());
    }

    // Получает список монтажников
    @GetMapping("employees/installers")
    public ResponseEntity<List<Employee>> getInstallersList() {
        return ResponseEntity.ok(employeeDispatcher.getInstallersList());
    }

    // Получает файл прикрепленный к задаче
    @GetMapping("attachment/{id}")
    public ResponseEntity<byte[]> getAttachmentFile(@PathVariable String id) {
        Attachment attachments = null;
        try {
            attachments = attachmentDispatcher.getAttachments(id);
        } catch (EntryNotFound e) {
            throw new ResponseException("Файл не найден");
        }
        Path filePath = Path.of(attachments.getPath());
        try {
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachments.getMimeType())).contentLength(attachments.getSize()).body(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new ResponseException("Не удалось прочитать файл");
        }
    }

    // Получает список файлов прикрепленных к задаче
    @GetMapping("task/{taskId}/attachments")
    public ResponseEntity<List<Attachment>> getAllTaskAttachments(@PathVariable Long taskId) {
        if (taskId == null) throw new ResponseException("Укажите корректный идентификатор задачи");
        return ResponseEntity.ok(attachmentDispatcher.getByTask(taskId));
    }

    // Получает количество файлов прикрепленных к задаче
    @GetMapping("task/{taskId}/attachments/count")
    public ResponseEntity<Integer> getAllTaskAttachmentsCount(@PathVariable Long taskId) {
        if (taskId == null) throw new ResponseException("Укажите корректный идентификатор задачи");
        return ResponseEntity.ok(attachmentDispatcher.getCountByTask(taskId));
    }

    // Назначает монтажников на задачу
    @PostMapping("task/{taskId}/assign-installers")
    public ResponseEntity<Void> assignInstallers(@PathVariable Long taskId, @RequestBody Set<Employee> installers, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Pair<Task, WorkLog> taskWorkLogPair = taskDispatcher.assignInstallers(taskId, installers, employeeFromRequest);
            telegramController.assignInstallers(taskWorkLogPair.getFirst(), taskWorkLogPair.getSecond().getEmployees(), employeeFromRequest);
            stompController.updateTask(taskWorkLogPair.getFirst());
            Set<Employee> observers = taskWorkLogPair.getFirst().getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.taskProcessed(taskWorkLogPair.getSecond()));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.createdWorkLog(taskWorkLogPair.getFirst(), taskWorkLogPair.getSecond(), employeeFromRequest));
            stompController.createTaskEvent(taskId, taskEvent);

        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // Принудительно забирает задачу у монтажников
    @PostMapping("task/{taskId}/force-close-work-log")
    public ResponseEntity<Void> forceCloseWorkLog(@PathVariable Long taskId, HttpServletRequest request) {
        try {
            Pair<Task, WorkLog> taskWorkPair = taskDispatcher.forceCloseWorkLog(taskId, getEmployeeFromRequest(request));
            stompController.updateTask(taskWorkPair.getFirst());

            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.forceCloseWorkLog(taskWorkPair.getFirst(), taskWorkPair.getSecond(), getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);

        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // Получить WorkLog по его идентификатору
    @GetMapping("work-log/{workLogId}")
    public ResponseEntity<WorkLog> getWorkLog(@PathVariable Long workLogId) {
        try {
            return ResponseEntity.ok(workLogDispatcher.get(workLogId));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Закрывает задачу
    @PatchMapping("task/{taskId}/close")
    public ResponseEntity<Task> closeTask(@PathVariable Long taskId, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task task = taskDispatcher.close(taskId);
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.taskClosed(task, employeeFromRequest));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.close(task, employeeFromRequest));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Вновь открывает задачу
    @PatchMapping("task/{taskId}/reopen")
    public ResponseEntity<Task> reopenTask(@PathVariable Long taskId, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task task = taskDispatcher.reopen(taskId);
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.taskReopened(task, employeeFromRequest));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.reopen(task, employeeFromRequest));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Редактирует информацию в задаче
    @PatchMapping("task/{taskId}/edit-fields")
    public ResponseEntity<Task> editTask(@PathVariable Long taskId, @RequestBody List<ModelItem> modelItems, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            TaskFieldsSnapshotDispatcher.SnapshotBuilder snapshotBuilder = taskFieldsSnapshotDispatcher.builder().beforeEditing(taskId, employeeFromRequest);
            Task task = taskDispatcher.edit(taskId, modelItems);
            snapshotBuilder.afterEditing().flush();
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employeeFromRequest);
            notificationDispatcher.createNotification(observers, Notification.taskEdited(task, employeeFromRequest));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.editFields(task, employeeFromRequest));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new ResponseException("Ошибка при обработке изменений");
        }
    }

    // Получает список с историей редактирования задачи
    @GetMapping("task/{taskId}/edit-snapshots")
    public ResponseEntity<List<TaskFieldsSnapshot>> getTaskEditSnapshots(@PathVariable Long taskId) {
        try {
            return ResponseEntity.ok(taskFieldsSnapshotDispatcher.getTaskFieldsSnapshots(taskId));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает список всех отделов с сотрудниками
    @GetMapping("departments")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentsDispatcher.getAll());
    }

    // Получает отдел по его идентификатору
    @GetMapping("department/{id}")
    public ResponseEntity<Department> getDepartment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(departmentsDispatcher.get(id));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Создает отдел
    @PostMapping("department")
    public ResponseEntity<Department> createDepartment(@RequestBody Map<String, String> body) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания отдела");
        if (body.get("name") == null || body.get("name").isBlank())
            throw new ResponseException("В запросе нет имени отдела");
        return ResponseEntity.ok(departmentsDispatcher.create(body.get("name"), body.get("description")));
    }

    // Редактирует отдел
    @PatchMapping("department/{id}")
    public ResponseEntity<Department> editDepartment(@RequestBody Map<String, String> body, @PathVariable Long id) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания отдела");
        if (body.get("name") == null || body.get("name").isBlank())
            throw new ResponseException("В запросе нет имени отдела");
        try {
            return ResponseEntity.ok(departmentsDispatcher.edit(id, body.get("name"), body.get("description")));
        } catch (EntryNotFound e) {
            throw new ResponseException("Отдел с идентификатором " + id + " не найден в базе данных");
        }
    }

    // Удаляет отдел
    @DeleteMapping("department/{id}")
    public ResponseEntity<Department> deleteDepartment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(departmentsDispatcher.delete(id));
        } catch (EntryNotFound e) {
            throw new ResponseException("Отдел с идентификатором " + id + "не найден в базе данных");
        }
    }

    // Получает список всех должностей
    @GetMapping("positions")
    public ResponseEntity<List<Position>> getAllPositions() {
        return ResponseEntity.ok(positionDispatcher.getAll());
    }

    // Создает должность
    @PostMapping("position")
    public ResponseEntity<Position> createPosition(@RequestBody PositionForm body) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания должности");
        if (body.getName() == null || body.getName().isBlank())
            throw new ResponseException("В запросе нет названия должности");
        return ResponseEntity.ok(positionDispatcher.create(body.getName(), body.getDescription(), body.getAccess()));
    }

    // Редактирует должность
    @PatchMapping("position/{id}")
    public ResponseEntity<Position> editPosition(@RequestBody PositionForm body, @PathVariable Long id) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания должности");
        if (body.getName() == null || body.getName().isBlank())
            throw new ResponseException("В запросе нет названия должности");
        try {
            return ResponseEntity.ok(positionDispatcher.edit(id, body.getName(), body.getDescription(), body.getAccess()));
        } catch (EntryNotFound e) {
            throw new ResponseException("Должность с идентификатором " + id + " не найдена в базе данных");
        }
    }

    // Удаляет должность
    @DeleteMapping("position/{id}")
    public ResponseEntity<Position> deletePosition(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(positionDispatcher.delete(id));
        } catch (EntryNotFound e) {
            throw new ResponseException("Должность с идентификатором " + id + " не найдена в базе данных");
        }
    }

    // Получает сотрудника по логину
    @GetMapping("employee/{login}")
    public ResponseEntity<Employee> getEmployee(@PathVariable String login) {
        Employee employeeByLogin = null;
        try {
            employeeByLogin = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok(employeeByLogin);
    }

    // Создает сотрудника
    @PostMapping("employee")
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeForm body) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания сотрудника");
        if (body.getFirstName() == null || body.getFirstName().isBlank())
            throw new ResponseException("В запросе нет имени сотрудника");
        if (body.getLogin() == null || body.getLogin().isBlank()) throw new ResponseException("В запросе нет логина");
        if (body.getPassword() == null || body.getPassword().isBlank())
            throw new ResponseException("В запросе нет пароля");
        if (body.getDepartment() == null) throw new ResponseException("Сотруднику не присвоен отдел");
        if (body.getPosition() == null) throw new ResponseException("Сотруднику не присвоена должность");
        try {
            return ResponseEntity.ok(employeeDispatcher.create(body.getFirstName(), body.getLastName(), body.getSecondName(), body.getLogin(), body.getPassword(), body.getAccess(), body.getInternalPhoneNumber(), body.getTelegramUserId(), body.getDepartment(), body.getPosition(), body.getOffsite()));
        } catch (AlreadyExists e) {
            throw new ResponseException("Сотрудник с данным логином уже существует");
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Редактирует информацию о сотруднике
    @PatchMapping("employee/{login}")
    public ResponseEntity<Employee> editEmployee(@RequestBody EmployeeForm body, @PathVariable String login) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания сотрудника");
        if (body.getFirstName() == null || body.getFirstName().isBlank())
            throw new ResponseException("В запросе нет имени сотрудника");
        if (body.getPassword() == null || body.getPassword().isBlank())
            throw new ResponseException("В запросе нет пароля");
        if (body.getDepartment() == null) throw new ResponseException("Сотруднику не присвоен отдел");
        if (body.getPosition() == null) throw new ResponseException("Сотруднику не присвоена должность");
        try {
            Employee employee = employeeDispatcher.edit(body.getFirstName(), body.getLastName(), body.getSecondName(), login, body.getPassword(),
                    body.getAccess(), body.getInternalPhoneNumber(), body.getTelegramUserId(), body.getDepartment(), body.getPosition(), body.getOffsite());
            stompController.updateEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException("Сотрудник с логином " + login + " не найден в базе данных для редактирования");
        } catch (EditingNotPossible e) {
            throw new ResponseException("Сотрудник удален, не возможно отредактировать");
        }
    }

    // Изменить статус сотрудника
    @PatchMapping("employee/status")
    public ResponseEntity<Employee> changeEmployeeStatus(@RequestBody String status, HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        try {
            Employee employee = employeeDispatcher.changeStatus(currentUser.getLogin(), EmployeeStatus.valueOf(status));
            stompController.updateEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет сотрудника
    @DeleteMapping("employee/{login}")
    public ResponseEntity<Employee> deleteEmployee(@PathVariable String login) {
        try {
            Employee employee = employeeDispatcher.delete(login);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException("Сотрудник с логином " + login + " не найден в базе данных");
        }
    }

    // Получает информацию о себе
    @GetMapping("me")
    public ResponseEntity<Employee> getMe(HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        return ResponseEntity.ok(currentUser);
    }

    // Получает аватар пользователя
    @GetMapping("avatar/{file}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String file) {
        Path filePath = Path.of(".\\attachments", "avatars", file);
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(Files.probeContentType(filePath))).contentLength(bytes.length).body(bytes);
        } catch (IOException e) {
            throw new ResponseException("Не удалось прочитать файл");
        }
    }

    // Устанавливает аватар
    @PostMapping("employee/{login}/avatar")
    public ResponseEntity<Employee> setAvatar(@PathVariable String login, @RequestBody Map<String, String> body) {
        Employee employee = null;
        try {
            employee = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }

        byte[] imageBytes = Base64.getDecoder().decode(body.get("avatar").replaceAll("data:[^\\/]+\\/[^;]+;base64,", "").getBytes(StandardCharsets.UTF_8));
        String fileName = UUID.randomUUID() + ".png";
        Path filePath = Path.of(".\\attachments", "avatars", fileName);

        try {
            Files.write(filePath, imageBytes);
            employee.setAvatar(fileName);
            employeeDispatcher.unsafeSave(employee);
            stompController.updateEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (IOException e) {
            throw new ResponseException("Не удалось сохранить файл");
        }
    }

    // Получает список уведомлений пользователя
    @GetMapping("notifications")
    public ResponseEntity<Page<Notification>> getNotifications(@RequestParam Long first, @RequestParam Integer limit, @RequestParam Boolean unreadOnly, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(notificationDispatcher.getNotifications(getEmployeeFromRequest(request).getLogin(), first, limit, unreadOnly));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает количество непрочитанных уведомлений
    @GetMapping("notifications/unread-count")
    public ResponseEntity<Long> getUnreadNotificationsCount(HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            return ResponseEntity.ok(notificationDispatcher.getUnreadCount(employee.getLogin()));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Устанавливает все уведомления как прочитанные
    @PatchMapping("notifications/read-all")
    public ResponseEntity<Void> setAllNotificationsAsRead(HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            notificationDispatcher.setAllAsRead(employee.getLogin());
            return ResponseEntity.ok().build();
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Отправляет сообщение в чат
    @PostMapping("chat/{chatId}/message")
    public ResponseEntity<Void> sendMessage(@PathVariable Long chatId, @RequestBody MessageData message, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            Pair<Chat, ChatMessage> chatMessage = chatDispatcher.sendToChat(chatId, ChatMessage.of(message, employee));
            stompController.createMessage(chatMessage.getSecond());
            telegramController.broadcastChatMessage(chatMessage.getFirst(), chatMessage.getSecond());
            return ResponseEntity.ok().build();
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает страницу с сообщениями из чата
    @GetMapping("chat/{chatId}/messages")
    public ResponseEntity<Page<ChatMessage>> getChatMessages(@PathVariable Long chatId, @RequestParam Long first, @RequestParam Integer limit, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            return ResponseEntity.ok(chatDispatcher.getChatMessages(chatId, first, limit));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает активный чат задачи по taskId
    @GetMapping("task/{taskId}/active-chat")
    public ResponseEntity<Chat> getActiveChat(@PathVariable Long taskId, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            WorkLog activeWorkLog = workLogDispatcher.getActiveWorkLogByTaskId(taskId);
            return ResponseEntity.ok(activeWorkLog.getChat());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Отправляет список адресов как подсказки к автодополнению
    @GetMapping("suggestions/address")
    public ResponseEntity<List<AddressDto>> getAddressSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(addressDispatcher.getSuggestions(query));
    }

    // Получает объект парсера трекера
    @GetMapping("parser/tracker")
    public ResponseEntity<OldTracker> getTrackerParser() {
        return ResponseEntity.ok(oldTracker);
    }

    // Сигнал парсеру трекера начать
    @PostMapping("parser/tracker/start")
    public ResponseEntity<Void> startTrackerParser() {
        try {
            oldTracker.startParse();
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            throw new ResponseException("Не удалось начать сбор заявок");
        }
    }

    // Сигнал парсеру трекера остановится
    @PostMapping("parser/tracker/stop")
    public ResponseEntity<Void> stopTrackerParser() {
        oldTracker.stopParse();
        return ResponseEntity.ok().build();
    }

    // Изменение настроек парсера трекера
    @PatchMapping("parser/tracker/settings")
    public ResponseEntity<Void> changeTrackerSettings(@RequestBody OldTrackerParserSettings settings) {
        oldTracker.setSettings(settings);
        return ResponseEntity.ok().build();
    }

    // Получение пула исправления не валидных адресов
    @GetMapping("parser/tracker/address-correcting-pool")
    public ResponseEntity<AddressCorrectingPool> getAddressCorrectingPool() {
        return ResponseEntity.ok(oldTracker.getAddressCorrectingPool());
    }

    // Создание задач из отредактированных адресов
    @PostMapping("parser/tracker/address-correcting-pool")
    public ResponseEntity<Void> receiveCorrectedAddressPool(@RequestBody AddressCorrectingPool pool) {
        oldTracker.createTasksFromCorrectedAddresses(pool);
        return ResponseEntity.ok().build();
    }

    // Сигнал парсеру адресов начать
    @PostMapping("parser/addresses/start")
    public ResponseEntity<Void> startAddressesParser() {
        addressParser.startParse();
        return ResponseEntity.ok().build();
    }


    private Employee getEmployeeFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) throw new ResponseException("Не авторизован");
        String login = AuthorizationProvider.getLoginFromCookie(List.of(request.getCookies()));
        if (login == null) throw new ResponseException("Не удалось получить данные о текущем пользователе");

        Employee currentUser = null;
        try {
            currentUser = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }

        if (currentUser.getDeleted()) throw new ResponseException("Сотрудник удален");

        return currentUser;
    }
}
