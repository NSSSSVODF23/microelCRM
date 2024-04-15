package com.microel.trackerbackend.services.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microel.tdo.dynamictable.DynamicTableColumn;
import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.misc.WireframeTaskCounter;
import com.microel.trackerbackend.misc.sorting.TaskJournalSortingTypes;
import com.microel.trackerbackend.modules.transport.ChangeTaskObserversDTO;
import com.microel.trackerbackend.modules.transport.IDuration;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.mapper.ChatMapper;
import com.microel.trackerbackend.storage.dto.mapper.CommentMapper;
import com.microel.trackerbackend.storage.dto.task.TaskListDto;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.TaskJournalItem;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskFieldsSnapshot;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.Observer;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.TaskTypeDirectory;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Triplet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("api/private/task")
public class TaskRequestController {

    private final TaskDispatcher taskDispatcher;
    private final CommentDispatcher commentDispatcher;
    private final ModelItemDispatcher modelItemDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final AttachmentDispatcher attachmentDispatcher;
    private final TaskEventDispatcher taskEventDispatcher;
    private final StompController stompController;
    private final TaskFieldsSnapshotDispatcher taskFieldsSnapshotDispatcher;
    private final NotificationDispatcher notificationDispatcher;
    private final WorkLogDispatcher workLogDispatcher;
    private final TelegramController telegramController;

    public TaskRequestController(TaskDispatcher taskDispatcher,
                                 CommentDispatcher commentDispatcher,
                                 ModelItemDispatcher modelItemDispatcher,
                                 EmployeeDispatcher employeeDispatcher,
                                 AttachmentDispatcher attachmentDispatcher,
                                 TaskEventDispatcher taskEventDispatcher,
                                 StompController stompController,
                                 TaskFieldsSnapshotDispatcher taskFieldsSnapshotDispatcher,
                                 NotificationDispatcher notificationDispatcher,
                                 WorkLogDispatcher workLogDispatcher,
                                 TelegramController telegramController) {
        this.taskDispatcher = taskDispatcher;
        this.commentDispatcher = commentDispatcher;
        this.modelItemDispatcher = modelItemDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.attachmentDispatcher = attachmentDispatcher;
        this.taskEventDispatcher = taskEventDispatcher;
        this.stompController = stompController;
        this.taskFieldsSnapshotDispatcher = taskFieldsSnapshotDispatcher;
        this.notificationDispatcher = notificationDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.telegramController = telegramController;
    }

    // Получение страницу с задачами используя фильтрацию
    @PostMapping("page/{page}")
    public ResponseEntity<Page<TaskListDto>> getTasks(@PathVariable Integer page, @Nullable @RequestBody TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        if (condition == null) {
            return ResponseEntity.ok(taskDispatcher.getTasks(page, 15, null, null).map(TaskListDto::of));
        }
        condition.clean();

        Instant startDBRequest = Instant.now();

        Page<Task> tasks;
        if (condition.getOnlyMy() != null && condition.getOnlyMy()) {
            tasks = taskDispatcher.getTasks(page, 15, condition, employee);
        } else {
            tasks = taskDispatcher.getTasks(page, 15, condition, null);
        }

        Instant endDBRequest = Instant.now();
        Duration dbDuration = Duration.between(startDBRequest, endDBRequest);

        return ResponseEntity.ok()
                .header("Server-Timing", "db;desc=\"DB Reading\";dur=" + dbDuration.toMillis())
                .body(tasks.map(TaskListDto::of));
    }

    @GetMapping("{id}/type-list/available-to-change")
    public ResponseEntity<List<TaskStage>> getAvailableTaskTypesToChange(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getAvailableTaskTypesToChange(id));
    }

    @GetMapping("{id}/directory-list/available-to-change")
    public ResponseEntity<List<TaskTypeDirectory>> getAvailableDirectoriesToChange(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getAvailableTaskDirectoryToChange(id));
    }

    // Получает страницу с задачами принадлежащими текущему наблюдателю
    @GetMapping("page/incoming/{page}")
    public ResponseEntity<Page<Task>> getIncomingTasks(@PathVariable Integer page, TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        condition.clean();
        return ResponseEntity.ok(taskDispatcher.getIncomingTasks(page, condition, employee));
    }

    @GetMapping("{taskId}/check-compatibility/{otTaskId}")
    public ResponseEntity<Map<String, Object>> checkCompatibility(@PathVariable Long taskId, @PathVariable Long otTaskId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            taskDispatcher.checkCompatibility(taskId, otTaskId, employee);
            return ResponseEntity.ok(null);
        } catch (ResponseException e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("{taskId}/connect-to/{otTaskId}")
    public ResponseEntity<Void> connectToOldTracker(@PathVariable Long taskId, @PathVariable Long otTaskId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        taskDispatcher.connectToOldTracker(taskId, otTaskId, employee);
        return ResponseEntity.ok().build();
    }

    //changeTaskStageInOldTracker(taskId: number, taskStageId: number) {
    //        return this.sendPatch(`api/private/${taskId}/old-tracker-stage/${taskStageId}/change`, {});
    //    }
    @PatchMapping("{taskId}/old-tracker-stage/{taskStageId}/change")
    public ResponseEntity<Void> changeTaskStageInOldTracker(@PathVariable Long taskId, @PathVariable Integer taskStageId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        taskDispatcher.changeTaskStageInOldTracker(taskId, taskStageId, employee);
        return ResponseEntity.ok().build();
    }

    @GetMapping("page/by-login/{login}")
    public ResponseEntity<Page<Task>> getTasksByLogin(@PathVariable String login, @RequestParam Integer page) {
        return ResponseEntity.ok(taskDispatcher.getTasksByLogin(login, page, 5));
    }

    // Создание новой задачи
    @PostMapping("")
    public ResponseEntity<Task> createTask(@RequestBody Task.CreationBody body, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        try {
            // Создаём задачу в базе данных
            Task createdTask = taskDispatcher.createTask(body, employee);

            Set<Employee> observers = createdTask.getAllEmployeesObservers(employee);
            // Создаем оповещение о новой задаче
            notificationDispatcher.createNotification(observers, Notification.taskCreated(createdTask));

            createdTask.getAllEmployeesObservers().forEach(observer -> {
                WireframeTaskCounter wireframeTaskCounter = new WireframeTaskCounter();
                Long wireframeId = createdTask.getModelWireframe().getWireframeId();
                Long incomingTasksCount = taskDispatcher.getIncomingTasksCount(observer, wireframeId);
                wireframeTaskCounter.setId(wireframeId);
                wireframeTaskCounter.setNum(incomingTasksCount);
                stompController.updateIncomingTaskCounter(observer.getLogin(), wireframeTaskCounter);
            });

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
        } catch (IllegalFields | EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаление задачи
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.deleteTask(id, employee);
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskDeleted(task, employee));
        } catch (EntryNotFound e) {
            throw new ResponseException("Задача с идентификатором " + id + " не найдена в базе данных");
        }
        return ResponseEntity.ok().build();
    }

    // Получение задачи по идентификатору
    @GetMapping("{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(taskDispatcher.getTask(id));
        } catch (EntryNotFound e) {
            throw new ResponseException("Задача c id:" + id + " не найдена");
        }
    }

    // Получение коренного родителя задачи по идентификатору
    @GetMapping("{id}/root")
    public ResponseEntity<Task> getRootTask(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(taskDispatcher.getRootTask(id));
        } catch (EntryNotFound e) {
            throw new ResponseException("Задача c id:" + id + " не найдена");
        }
    }

    // Получение полей задачи по идентификатору
    @GetMapping("{id}/fields")
    public ResponseEntity<List<ModelItem>> getTaskFields(@PathVariable Long id) {
        return ResponseEntity.ok(modelItemDispatcher.getFieldsTask(id));
    }

    // Получает количество задач принадлежащих текущему наблюдателю
    @GetMapping("incoming/count")
    public ResponseEntity<Long> getCountIncomingTasks(HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCount(employee));
    }

    // Получает количество задач принадлежащих текущему наблюдателю отфильтрованы по шаблонам
    @GetMapping("incoming/wireframe/{wireframeId}/count")
    public ResponseEntity<Long> getCountIncomingTasksWireframe(@PathVariable Long wireframeId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCount(employee, wireframeId));
    }

    @GetMapping("wireframe/by-tags/count")
    public ResponseEntity<Map<Long, Long>> getCountTasksWireframeByTag(@RequestParam List<Long> wireframeIds) {
        return ResponseEntity.ok(taskDispatcher.getTasksCountByTags(wireframeIds));
    }

    @GetMapping("incoming/wireframe/by-tags/count")
    public ResponseEntity<Map<Long, Long>> getCountIncomingTasksWireframeByTag(@RequestParam List<Long> wireframeIds, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCountByTags(employee, wireframeIds));
    }

    @GetMapping("incoming/wireframe/{wireframeId}/by-stages/count")
    public ResponseEntity<Map<String, Long>> getCountIncomingTasksByStages(@PathVariable Long wireframeId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCountByStages(employee, wireframeId));
    }

    @GetMapping("wireframe/{wireframeId}/by-stages/count")
    public ResponseEntity<Map<String, Long>> getCountTasksByStages(@PathVariable Long wireframeId) {
        return ResponseEntity.ok(taskDispatcher.getTasksCountByStages(wireframeId));
    }

    @PostMapping("count")
    public ResponseEntity<Long> getCountTasks(@RequestBody TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
//        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getTasksCount(condition));
    }

    // Получает количество всех не закрытых задач по шаблонам
    @GetMapping("wireframe/{wireframeId}/count")
    public ResponseEntity<Long> getCountTasksWireframe(@PathVariable Long wireframeId) {
        return ResponseEntity.ok(taskDispatcher.getTasksCount(wireframeId));
    }

    // Получает список с задачами запланированных на определенный период
    @GetMapping("list/scheduled")
    public ResponseEntity<List<Task>> getScheduledTasks(@RequestParam Timestamp start, @RequestParam Timestamp end,
                                                        HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getScheduledTasks(employee, start, end));
    }

    // Получение страницы с задачами которые не находятся ни в одной стадии
    @GetMapping("page/template/{templateId}/stage")
    public ResponseEntity<Page<Task>> getActiveTasksByNullStage(@PathVariable Long templateId, @RequestParam Long offset, @RequestParam Integer limit) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTasksByStage(templateId, null, offset, limit));
    }

    // Получение страницы с задачами из конкретной стадии
    @GetMapping("page/template/{templateId}/stage/{stageId}")
    public ResponseEntity<Page<Task>> getActiveTasksByStage(@PathVariable Long templateId, @PathVariable String stageId, @RequestParam Long offset, @RequestParam Integer limit) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTasksByStage(templateId, stageId, offset, limit));
    }

    // Получение списка идентификаторов задач без стадии
    @GetMapping("template/{templateId}/stage/taskIdOnly")
    public ResponseEntity<List<Long>> getActiveTaskIdsByNullStage(@PathVariable Long templateId) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTaskIdsByStage(templateId, null));
    }

    // Получение списка идентификаторов задач из конкретной стадии
    @GetMapping("template/{templateId}/stage/{stageId}/taskIdOnly")
    public ResponseEntity<List<Long>> getActiveTaskIdsByStage(@PathVariable Long templateId, @PathVariable String stageId) {
        return ResponseEntity.ok().body(taskDispatcher.getActiveTaskIdsByStage(templateId, stageId));
    }

    // Изменение стадии задачи
    @PatchMapping("{taskId}/stage")
    public ResponseEntity<Task> changeTaskStage(@PathVariable Long taskId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = employeeDispatcher.getEmployeeFromRequest(request);
            Task task = taskDispatcher.changeTaskStage(taskId, body.get("stageId"));

            Long wireframeId = task.getModelWireframe().getWireframeId();

            task.getAllEmployeesObservers().forEach(observer -> {
                Long incomingTasksCount = taskDispatcher.getIncomingTasksCount(observer, wireframeId);
                Map<String, Long> incomingTasksCountByStages = taskDispatcher.getIncomingTasksCountByStages(observer, wireframeId);
                stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
                Map<Long, Map<Long, Long>> incomingTasksCountByTags = taskDispatcher.getIncomingTasksCountByTags(observer);
                stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
            });

            Long tasksCount = taskDispatcher.getTasksCount(task.getModelWireframe().getWireframeId());
            Map<String, Long> tasksCountByStages = taskDispatcher.getTasksCountByStages(wireframeId);
            stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
            Map<Long, Map<Long, Long>> tasksCountByTags = taskDispatcher.getTasksCountByTags();
            stompController.updateTagTaskCounter(tasksCountByTags);

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
    @PatchMapping("{taskId}/unlink-parent")
    public ResponseEntity<Task> unlinkParentTask(@PathVariable Long taskId, HttpServletRequest request) {
        try {
            Pair<Task, Task> taskTaskPair = taskDispatcher.unlinkFromParent(taskId);
            Employee employeeFromRequest = employeeDispatcher.getEmployeeFromRequest(request);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkFromParentTask(taskTaskPair.getFirst(), taskTaskPair.getSecond(), employeeFromRequest));
            TaskEvent parentTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkChildTask(taskTaskPair.getSecond(), Set.of(taskTaskPair.getFirst()), employeeFromRequest));
            stompController.createTaskEvent(taskId, taskEvent);
            stompController.createTaskEvent(taskTaskPair.getSecond().getTaskId(), parentTaskEvent);
            return ResponseEntity.ok(taskTaskPair.getFirst());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Устанавливает родительскую задачу
    @PatchMapping("{taskId}/link-to-parent")
    public ResponseEntity<Task> changeLinkToParentTask(@PathVariable Long taskId, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        try {
            Triplet<Task, Task, Task> taskTriplet = taskDispatcher.changeLinkToParentTask(taskId, body.get("parentTaskId"));
            Employee author = employeeDispatcher.getEmployeeFromRequest(request);
            TaskEvent targetTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToParentTask(taskTriplet.getValue0(), taskTriplet.getValue1(), author));
            stompController.createTaskEvent(taskId, targetTaskEvent);
            TaskEvent parentTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToChildTask(taskTriplet.getValue1(), Set.of(taskTriplet.getValue0()), author));
            stompController.createTaskEvent(taskTriplet.getValue1().getTaskId(), parentTaskEvent);
            if (taskTriplet.getValue2() != null) {
                TaskEvent previousParentTasksEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkChildTask(taskTriplet.getValue2(), Set.of(taskTriplet.getValue0()), author));
                stompController.createTaskEvent(taskTriplet.getValue2().getTaskId(), previousParentTasksEvent);
            }
            return ResponseEntity.ok(taskTriplet.getValue0());
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Добавляет в задачу ссылки на дочерние задачи
    @PatchMapping("{taskId}/append-links-to-children")
    public ResponseEntity<Task> appendLinksToChildrenTask(@PathVariable Long taskId, @RequestBody Set<Long> childIds, HttpServletRequest request) {
        try {
            Triplet<Task, List<Task>, List<Pair<Task, Task>>> complexOfTasks = taskDispatcher.appendLinksToChildrenTask(taskId, childIds);
            Employee author = employeeDispatcher.getEmployeeFromRequest(request);
            TaskEvent targetTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToChildTask(complexOfTasks.getValue0(), new HashSet<>(complexOfTasks.getValue1()), author));
            stompController.createTaskEvent(taskId, targetTaskEvent);
            complexOfTasks.getValue1().forEach(childTasks -> {
                TaskEvent childTaskEvent = taskEventDispatcher.appendEvent(TaskEvent.linkedToParentTask(childTasks, complexOfTasks.getValue0(), author));
                stompController.createTaskEvent(childTasks.getTaskId(), childTaskEvent);
            });
            complexOfTasks.getValue2().forEach(previousParentChild -> {
                TaskEvent previousParentTasksEvent = taskEventDispatcher.appendEvent(TaskEvent.unlinkChildTask(previousParentChild.getFirst(), Set.of(previousParentChild.getSecond()), author));
                stompController.createTaskEvent(previousParentChild.getFirst().getTaskId(), previousParentTasksEvent);
            });
            return ResponseEntity.ok(complexOfTasks.getValue0());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Устанавливает теги задачи
    @PatchMapping("{taskId}/tags")
    public ResponseEntity<Task> changeTaskTags(@PathVariable Long taskId, @RequestBody Set<TaskTag> body, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.modifyTags(taskId, body);

            task.getAllEmployeesObservers().forEach(observer -> {
                Map<Long, Map<Long, Long>> incomingTasksCountByTags = taskDispatcher.getIncomingTasksCountByTags(observer);
                stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
            });
            Map<Long, Map<Long, Long>> tasksCountByTags = taskDispatcher.getTasksCountByTags();
            stompController.updateTagTaskCounter(tasksCountByTags);

            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeTags(task, body, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает страницу с комментариями и событиями в конкретной задаче
    @GetMapping("{id}/journal")
    public ResponseEntity<Page<TaskJournalItem>> getTaskJournal(@PathVariable Long id, @RequestParam Long offset, @RequestParam @Nullable TaskJournalSortingTypes sorting, @RequestParam Integer limit) {
        Page<Comment> comments = commentDispatcher.getComments(id, offset, limit, sorting).map(CommentMapper::fromDto);
        List<TaskJournalItem> commentItems = comments.getContent().stream().map(commentDto -> (TaskJournalItem) commentDto).collect(Collectors.toList());
        if (comments.getTotalElements() == 0L) {
            List<TaskEvent> taskEvents = taskEventDispatcher.getTaskEvents(id, sorting);
            // Concat events and comments
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).toList();
            commentItems.addAll(taskEventsItems);
            if (sorting != null) {
                switch (sorting) {
                    case CREATE_DATE_ASC -> commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated));
                    case CREATE_DATE_DESC ->
                            commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
                }
            } else {
                commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
            }
        } else if (comments.isLast()) {
            Comment firstComment = comments.getContent().get(0);
            List<TaskEvent> taskEvents = new ArrayList<>();
            if (offset == 0) {
                taskEvents = taskEventDispatcher.getTaskEvents(id, sorting);
            } else if (offset > 0) {
                if (sorting == TaskJournalSortingTypes.CREATE_DATE_ASC) {
                    taskEvents = taskEventDispatcher.getTaskEventsTo(id, firstComment.getCreated(), sorting);
                } else {
                    taskEvents = taskEventDispatcher.getTaskEventsFrom(id, firstComment.getCreated(), sorting);
                }
            }
            // Concat events and comments and sort by creation date
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).toList();
            commentItems.addAll(taskEventsItems);
            if (sorting != null) {
                switch (sorting) {
                    case CREATE_DATE_ASC -> commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated));
                    case CREATE_DATE_DESC ->
                            commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
                }
            } else {
                commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
            }
        } else {
            Comment firstComment = comments.getContent().get(0);
            Comment lastComment = comments.getContent().get(comments.getContent().size() - 1);
            List<TaskEvent> taskEvents = new ArrayList<>();
            if (offset == 0) {
                if (sorting == TaskJournalSortingTypes.CREATE_DATE_ASC) {
                    taskEvents = taskEventDispatcher.getTaskEventsFrom(id, firstComment.getCreated(), sorting);
                } else {
                    taskEvents = taskEventDispatcher.getTaskEventsTo(id, lastComment.getCreated(), sorting);
                }
            } else if (offset > 0) {
                taskEvents = taskEventDispatcher.getTaskEvents(id, firstComment.getCreated(), lastComment.getCreated(), sorting);
            }
            // Concat events and comments and sort by creation date
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).toList();
            commentItems.addAll(taskEventsItems);
            if (sorting != null) {
                switch (sorting) {
                    case CREATE_DATE_ASC -> commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated));
                    case CREATE_DATE_DESC ->
                            commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
                }
            } else {
                commentItems.sort(Comparator.comparing(TaskJournalItem::getCreated).reversed());
            }
        }

        return ResponseEntity.ok(new PageImpl<>(commentItems, Pageable.unpaged(), comments.getTotalElements()));
    }

    // Изменяет наблюдателей в задаче
    @PatchMapping("{id}/observers")
    public ResponseEntity<Task> changeTaskObservers(@PathVariable Long id, @RequestBody ChangeTaskObserversDTO body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = employeeDispatcher.getEmployeeFromRequest(request);
            Task previousTask = taskDispatcher.getTask(id);
            Set<Employee> previousObservers = new HashSet<>(previousTask.getAllEmployeesObservers());

            Task task = taskDispatcher.changeTaskObservers(id, body.getDepartmentObservers(), body.getPersonalObservers());
            Set<Employee> newObservers = new HashSet<>(task.getAllEmployeesObservers(employeeFromRequest));

            // Получаем Set новых наблюдателей из разницы newObservers и previousObserver
            Set<Employee> employeesObservers = new HashSet<>(newObservers);
            employeesObservers.removeAll(previousObservers);
            employeesObservers.remove(employeeFromRequest);

            notificationDispatcher.createNotification(employeesObservers, Notification.youObserver(task));

            List<Observer> observers = new ArrayList<>();
            observers.addAll(task.getEmployeesObservers());
            observers.addAll(task.getDepartmentsObservers());
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeObservers(task, observers, employeeFromRequest));

            Long wireframeId = task.getModelWireframe().getWireframeId();

            Set<Employee> allEmployeesObservers = task.getAllEmployeesObservers();
            allEmployeesObservers.addAll(previousObservers);

            allEmployeesObservers.forEach(observer -> {
                Long incomingTasksCount = taskDispatcher.getIncomingTasksCount(observer, wireframeId);
                Map<String, Long> incomingTasksCountByStages = taskDispatcher.getIncomingTasksCountByStages(observer, wireframeId);
                stompController.updateIncomingTaskCounter(observer.getLogin(), WireframeTaskCounter.of(wireframeId, incomingTasksCount, incomingTasksCountByStages));
                Map<Long, Map<Long, Long>> incomingTasksCountByTags = taskDispatcher.getIncomingTasksCountByTags(observer);
                stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
            });

            Long tasksCount = taskDispatcher.getTasksCount(task.getModelWireframe().getWireframeId());
            Map<String, Long> tasksCountByStages = taskDispatcher.getTasksCountByStages(wireframeId);
            stompController.updateTaskCounter(WireframeTaskCounter.of(wireframeId, tasksCount, tasksCountByStages));
            Map<Long, Map<Long, Long>> tasksCountByTags = taskDispatcher.getTasksCountByTags();
            stompController.updateTagTaskCounter(tasksCountByTags);

            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет ответственного в задаче
    @PatchMapping("{id}/responsible")
    public ResponseEntity<Task> changeTaskResponsible(@PathVariable Long id, @RequestBody Employee body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = employeeDispatcher.getEmployeeFromRequest(request);
            Task task = taskDispatcher.changeTaskResponsible(id, body);
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
    @PatchMapping("{id}/unbind-responsible")
    public ResponseEntity<Task> unbindTaskResponsible(@PathVariable Long id, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.unbindTaskResponsible(id);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.unbindResponsible(task, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет дату начала выполнения задачи
    @PatchMapping("{id}/actual-from")
    public ResponseEntity<Task> changeTaskActualFrom(@PathVariable Long id, @RequestBody Instant body, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.changeTaskActualFrom(id, body);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeActualFrom(task, body, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Изменяет дату окончания выполнения задачи
    @PatchMapping("{id}/actual-to")
    public ResponseEntity<Task> changeTaskActualTo(@PathVariable Long id, @RequestBody Instant body, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.changeTaskActualTo(id, body);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeActualTo(task, body, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Сдвигает даты актуальности на определенный промежуток времени
    @PatchMapping("{taskId}/scheduled/move")
    public ResponseEntity<Task> moveTaskScheduled(@PathVariable Long taskId, @RequestBody IDuration delta, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.moveTaskScheduled(taskId, delta);
            TaskEvent taskEvent;
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

    // Удаляет дату начала выполнения задачи
    @PatchMapping("{id}/clear-actual-from-date")
    public ResponseEntity<Task> clearTaskActualFromDate(@PathVariable Long id, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.clearTaskActualFrom(id);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.clearActualFrom(task, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет дату окончания выполнения задачи
    @PatchMapping("{id}/clear-actual-to-date")
    public ResponseEntity<Task> clearTaskActualToDate(@PathVariable Long id, HttpServletRequest request) {
        try {
            Task task = taskDispatcher.clearTaskActualTo(id);
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.clearActualTo(task, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(id, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает список файлов прикрепленных к задаче
    @GetMapping("{taskId}/attachments")
    public ResponseEntity<List<Attachment>> getAllTaskAttachments(@PathVariable Long taskId) {
        if (taskId == null) throw new ResponseException("Укажите корректный идентификатор задачи");
        return ResponseEntity.ok(attachmentDispatcher.getByTask(taskId));
    }

    // Получает количество файлов прикрепленных к задаче
    @GetMapping("{taskId}/attachments/count")
    public ResponseEntity<Integer> getAllTaskAttachmentsCount(@PathVariable Long taskId) {
        if (taskId == null) throw new ResponseException("Укажите корректный идентификатор задачи");
        return ResponseEntity.ok(attachmentDispatcher.getCountByTask(taskId));
    }

    // Назначает монтажников на задачу
    @PostMapping("{taskId}/assign-installers")
    public ResponseEntity<Void> assignInstallers(@PathVariable Long taskId, @RequestBody WorkLog.AssignBody body, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            WorkLog workLog = taskDispatcher.assignInstallers(taskId, body, employee);
            if(workLog.getScheduled() == null) {
                List<Employee> acceptedEmployees = workLogDispatcher.getAcceptedEmployees(workLog.getEmployees());
                telegramController.assignInstallers(workLog, employee, acceptedEmployees);
            }
            stompController.createWorkLog(workLog);
            stompController.createChat(Objects.requireNonNull(ChatMapper.toDto(workLog.getChat())));
            Set<Employee> observers = workLog.getTask().getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskProcessed(workLog));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.createdWorkLog(workLog.getTask(), workLog, employee));
            stompController.createTaskEvent(taskId, taskEvent);
        } catch (Throwable e) {
            taskDispatcher.abortAssignation(taskId, employee);
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // Принудительно забирает задачу у монтажников
    @PostMapping("{taskId}/force-close-work-log")
    public ResponseEntity<Void> forceCloseWorkLog(@PathVariable Long taskId, @RequestBody String reasonOfClosing, HttpServletRequest request) {
        try {
            WorkLog taskWorkPair = taskDispatcher.forceCloseWorkLog(taskId, reasonOfClosing, employeeDispatcher.getEmployeeFromRequest(request));
            stompController.closeChat(taskWorkPair.getChat());
            stompController.closeWorkLog(taskWorkPair);

            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.forceCloseWorkLog(taskWorkPair.getTask(), taskWorkPair, employeeDispatcher.getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);

        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("move-to-directory")
    public ResponseEntity<Void> moveTaskToDirectory(@RequestBody TaskDispatcher.MovingToDirectoryForm form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        List<Task> tasks = taskDispatcher.moveToDirectory(form);
        for (Task task : tasks) {
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskMovedToDirectory(task, employee));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.movedToDirectory(task, employee));
            stompController.createTaskEvent(task.getTaskId(), taskEvent);
        }
        return ResponseEntity.ok().build();
    }

    // Закрывает задачу
    @PatchMapping("{taskId}/close")
    public ResponseEntity<Task> closeTask(@PathVariable Long taskId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.close(taskId, employee, true);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Вновь открывает задачу
    @PatchMapping("{taskId}/reopen")
    public ResponseEntity<Task> reopenTask(@PathVariable Long taskId, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.reopen(taskId, employee);

            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Редактирует информацию в задаче
    @PatchMapping("{taskId}/edit-fields")
    public ResponseEntity<Task> editTask(@PathVariable Long taskId, @RequestBody List<ModelItem> modelItems, HttpServletRequest request) {
        try {
            Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
            TaskFieldsSnapshotDispatcher.SnapshotBuilder snapshotBuilder = taskFieldsSnapshotDispatcher.builder().beforeEditing(taskId, employee);
            Task task = taskDispatcher.edit(taskId, modelItems, employee);
            snapshotBuilder.afterEditing().flush();
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskEdited(task, employee));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.editFields(task, employee));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new ResponseException("Ошибка при обработке изменений");
        }
    }

    // Получает список с историей редактирования задачи
    @GetMapping("{taskId}/edit-snapshots")
    public ResponseEntity<List<TaskFieldsSnapshot>> getTaskEditSnapshots(@PathVariable Long taskId) {
        try {
            return ResponseEntity.ok(taskFieldsSnapshotDispatcher.getTaskFieldsSnapshots(taskId));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает активный чат задачи по taskId
    @GetMapping("{taskId}/active-chat")
    public ResponseEntity<Chat> getActiveChat(@PathVariable Long taskId) {
        try {
            WorkLog activeWorkLog = workLogDispatcher.getActiveWorkLogByTaskId(taskId);
            return ResponseEntity.ok(activeWorkLog.getChat());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    /**
     * Получить заголовки таблицы реестра задач
     */
    @GetMapping("registry/headers/{taskStatus}/{taskClass}")
    public ResponseEntity<List<DynamicTableColumn>> getRegistryHeaders(@PathVariable TaskStatus taskStatus, @PathVariable Long taskClass) {
        return ResponseEntity.ok(taskDispatcher.getRegistryTableHeaders(taskStatus, taskClass));
    }

    @PostMapping("registry/content/{taskStatus}/{taskClass}")
    public ResponseEntity<Page<TaskDispatcher.RegistryTableEntry>> getTaskRegistryTableContent(@PathVariable TaskStatus taskStatus, @PathVariable Long taskClass, @RequestBody TaskDispatcher.TaskRegistryRequestBody body) {
        return ResponseEntity.ok(taskDispatcher.getRegistryTableContent(taskStatus, taskClass, body));
    }

}
