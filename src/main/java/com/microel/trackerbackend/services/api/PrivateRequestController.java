package com.microel.trackerbackend.services.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.*;
import com.microel.trackerbackend.misc.accounting.MonthlySalaryReportTable;
import com.microel.trackerbackend.misc.accounting.TDocumentFactory;
import com.microel.trackerbackend.misc.network.NetworkRemoteControl;
import com.microel.trackerbackend.misc.sorting.TaskJournalSortingTypes;
import com.microel.trackerbackend.modules.transport.ChangeTaskObserversDTO;
import com.microel.trackerbackend.modules.transport.IDuration;
import com.microel.trackerbackend.parsers.addresses.AddressParser;
import com.microel.trackerbackend.parsers.oldtracker.AddressCorrectingPool;
import com.microel.trackerbackend.parsers.oldtracker.OldTracker;
import com.microel.trackerbackend.parsers.oldtracker.OldTrackerParserSettings;
import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.services.FilesWatchService;
import com.microel.trackerbackend.services.PhyPhoneService;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.types.*;
import com.microel.trackerbackend.services.external.billing.BillingPayType;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerService;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskClassOT;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.dto.mapper.ChatMapper;
import com.microel.trackerbackend.storage.dto.mapper.CommentMapper;
import com.microel.trackerbackend.storage.dto.task.TaskListDto;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.SuperMessage;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.TaskJournalItem;
import com.microel.trackerbackend.storage.entities.comments.dto.CommentData;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.equipment.ClientEquipment;
import com.microel.trackerbackend.storage.entities.filesys.FileSystemItem;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskFieldsSnapshot;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.Observer;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.*;
import com.microel.trackerbackend.storage.entities.templating.*;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.exceptions.*;
import com.microel.trackerbackend.storage.repositories.PhyPhoneInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.javatuples.Triplet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("api/private")
public class PrivateRequestController {
    private final PhyPhoneInfoRepository phyPhoneInfoRepository;
    private final WireframeDispatcher wireframeDispatcher;
    private final TaskDispatcher taskDispatcher;
    private final StreetDispatcher streetDispatcher;
    private final HouseDispatcher houseDispatcher;
    private final CityDispatcher cityDispatcher;
    private final CommentDispatcher commentDispatcher;
    private final ModelItemDispatcher modelItemDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final AttachmentDispatcher attachmentDispatcher;
    private final DepartmentDispatcher departmentsDispatcher;
    private final PositionDispatcher positionDispatcher;
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
    private final PaidActionDispatcher paidActionDispatcher;
    private final PaidWorkGroupDispatcher paidWorkGroupDispatcher;
    private final PaidWorkDispatcher paidWorkDispatcher;
    private final WorkCalculationDispatcher workCalculationDispatcher;
    private final WorkingDayDispatcher workingDayDispatcher;
    private final ApiBillingController apiBillingController;
    private final AcpClient acpClient;
    private final ClientEquipmentDispatcher clientEquipmentDispatcher;
    private final FilesWatchService filesWatchService;
    private final PhyPhoneService phyPhoneService;
    private final OldTrackerService oldTrackerService;

    public PrivateRequestController(WireframeDispatcher wireframeDispatcher, TaskDispatcher taskDispatcher,
                                    StreetDispatcher streetDispatcher, HouseDispatcher houseDispatcher, CityDispatcher cityDispatcher,
                                    CommentDispatcher commentDispatcher, ModelItemDispatcher modelItemDispatcher,
                                    EmployeeDispatcher employeeDispatcher, AttachmentDispatcher attachmentDispatcher,
                                    DepartmentDispatcher departmentsDispatcher, PositionDispatcher positionDispatcher,
                                    TaskEventDispatcher taskEventDispatcher, StompController stompController,
                                    TaskTagDispatcher taskTagDispatcher, TaskFieldsSnapshotDispatcher taskFieldsSnapshotDispatcher,
                                    NotificationDispatcher notificationDispatcher, WorkLogDispatcher workLogDispatcher,
                                    ChatDispatcher chatDispatcher, TelegramController telegramController,
                                    OldTracker oldTracker, AddressParser addressParser, AddressDispatcher addressDispatcher, PaidActionDispatcher paidActionDispatcher, PaidWorkGroupDispatcher paidWorkGroupDispatcher, PaidWorkDispatcher paidWorkDispatcher, WorkCalculationDispatcher workCalculationDispatcher, WorkingDayDispatcher workingDayDispatcher, ApiBillingController apiBillingController, AcpClient acpClient, ClientEquipmentDispatcher clientEquipmentDispatcher, FilesWatchService filesWatchService, PhyPhoneService phyPhoneService,
                                    PhyPhoneInfoRepository phyPhoneInfoRepository, OldTrackerService oldTrackerService) {
        this.wireframeDispatcher = wireframeDispatcher;
        this.taskDispatcher = taskDispatcher;
        this.streetDispatcher = streetDispatcher;
        this.houseDispatcher = houseDispatcher;
        this.cityDispatcher = cityDispatcher;
        this.commentDispatcher = commentDispatcher;
        this.modelItemDispatcher = modelItemDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.attachmentDispatcher = attachmentDispatcher;
        this.departmentsDispatcher = departmentsDispatcher;
        this.positionDispatcher = positionDispatcher;
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
        this.paidActionDispatcher = paidActionDispatcher;
        this.paidWorkGroupDispatcher = paidWorkGroupDispatcher;
        this.paidWorkDispatcher = paidWorkDispatcher;
        this.workCalculationDispatcher = workCalculationDispatcher;
        this.workingDayDispatcher = workingDayDispatcher;
        this.apiBillingController = apiBillingController;
        this.acpClient = acpClient;
        this.clientEquipmentDispatcher = clientEquipmentDispatcher;
        this.filesWatchService = filesWatchService;
        this.phyPhoneService = phyPhoneService;
        this.phyPhoneInfoRepository = phyPhoneInfoRepository;
        this.oldTrackerService = oldTrackerService;
    }

    // Получает список доступных наблюдателей из базы данных
    @GetMapping("available-observers")
    public ResponseEntity<List<DefaultObserver>> getAvailableObservers() {
        List<DefaultObserver> observers = new ArrayList<>();
        List<Employee> employees = employeeDispatcher.getEmployeesList(null, false, false);
        List<Department> departments = departmentsDispatcher.getAll();
        observers.addAll(employees.stream().map(DefaultObserver::from).toList());
        observers.addAll(departments.stream().map(DefaultObserver::from).toList());
        return ResponseEntity.ok(observers);
    }

    @GetMapping("available-observers/{query}")
    public ResponseEntity<List<DefaultObserver>> getAvailableObserversSuggestions(@PathVariable String query) {
        List<DefaultObserver> observers = new ArrayList<>();
        List<Employee> employees = employeeDispatcher.getEmployeesList(query, false, false);
        List<Department> departments = departmentsDispatcher.getAll().stream().filter(department -> department.getName().toLowerCase().contains(query.toLowerCase())).toList();
        observers.addAll(employees.stream().map(DefaultObserver::from).toList());
        observers.addAll(departments.stream().map(DefaultObserver::from).toList());
        return ResponseEntity.ok(observers);
    }

    // Создание шаблона задачи
    @PostMapping("wireframe")
    public ResponseEntity<Wireframe> createWireframe(@RequestBody Wireframe.Form body, HttpServletRequest request) {
        Wireframe wireframe = wireframeDispatcher.createWireframe(body, getEmployeeFromRequest(request));
        stompController.createWireframe(wireframe);
        return ResponseEntity.ok(wireframe);
    }

    // Редактирование шаблона задачи
    @PatchMapping("wireframe/{id}")
    public ResponseEntity<Wireframe> updateWireframe(@PathVariable Long id, @RequestBody Wireframe.Form body) {
        Wireframe wireframe = wireframeDispatcher.updateWireframe(id, body);
        taskDispatcher.restoreTasksToOriginalDirectory(wireframe);
        stompController.updateWireframe(wireframe);
        return ResponseEntity.ok(wireframe);
    }

    // Удаление шаблона задачи
    @DeleteMapping("wireframe/{id}")
    public ResponseEntity<Void> deleteWireframe(@PathVariable Long id) {
        try {
            Wireframe wireframe = wireframeDispatcher.deleteWireframe(id);
            stompController.deleteWireframe(wireframe);
            return ResponseEntity.ok().build();
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получение списка шаблонов
    @GetMapping("wireframes")
    public ResponseEntity<List<Wireframe>> getWireframes(@Nullable @RequestParam Boolean includingRemoved) {
        if (includingRemoved == null || !includingRemoved)
            return ResponseEntity.ok(wireframeDispatcher.getAllWireframes(false));
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
        List<FieldItem> fields = new ArrayList<>();
        wireframe.getSteps().forEach(step -> fields.addAll(step.getFields()));
        return ResponseEntity.ok(fields);
    }

    // Получение всех городов
    @GetMapping("cities")
    public ResponseEntity<List<City>> getAllCities() {
        return ResponseEntity.ok(cityDispatcher.getCities());
    }

    // Создание города
    @PostMapping("city")
    public ResponseEntity<City> createCity(@RequestBody City.Form form) {
        City city = cityDispatcher.create(form);
        return ResponseEntity.ok(city);
    }

    // Редактирование города
    @PatchMapping("city/{id}")
    public ResponseEntity<City> updateCity(@PathVariable Long id, @RequestBody City.Form form) {
        City city = cityDispatcher.edit(id, form);
        return ResponseEntity.ok(city);
    }

    // Удаление города
    @DeleteMapping("city/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityDispatcher.delete(id);
        return ResponseEntity.ok().build();
    }

    // Создание улицы
    @PostMapping("city/{id}/street")
    public ResponseEntity<Street> createStreet(@PathVariable Long id, @RequestBody Street.Form form) {
        Street street = streetDispatcher.create(id, form);
        return ResponseEntity.ok(street);
    }

    @GetMapping("suggestions/street")
    public ResponseEntity<List<Street.Suggestion>> getStreetSuggestions(@RequestParam @Nullable String query) {
        List<Street.Suggestion> suggestions = streetDispatcher.getSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    // Редактирование улицы
    @PatchMapping("street/{id}")
    public ResponseEntity<Street> updateStreet(@PathVariable Long id, @RequestBody Street.Form form) {
        Street street = streetDispatcher.edit(id, form);
        return ResponseEntity.ok(street);
    }

    // Удаление улицы
    @DeleteMapping("street/{id}")
    public ResponseEntity<Void> deleteStreet(@PathVariable Long id) {
        streetDispatcher.delete(id);
        return ResponseEntity.ok().build();
    }

    // Получение всех улиц в населенном пункте
    @GetMapping("streets/{cityId}")
    public ResponseEntity<List<Street>> getAllStreets(@PathVariable Long cityId, @Nullable @RequestParam String filter) {
        if (filter != null) return ResponseEntity.ok(streetDispatcher.lookupByFilter(filter, cityId));
        return ResponseEntity.ok(streetDispatcher.getStreetsInCity(cityId));
    }

    // Получаем список домов
    @GetMapping("houses/{streetId}")
    public ResponseEntity<List<House>> getAllHouses(@PathVariable Long streetId) {
        return ResponseEntity.ok(houseDispatcher.getByStreetId(streetId));
    }

    // Создание дома
    @PostMapping("street/{id}/house")
    public ResponseEntity<Address> createHouse(@PathVariable Long id, @RequestBody House.Form form) {
        House house = houseDispatcher.create(id, form);
        return ResponseEntity.ok(house.getAddress());
    }

    // Редактирование дома
    @PatchMapping("house/{id}")
    public ResponseEntity<House> updateHouse(@PathVariable Long id, @RequestBody House.Form form) {
        House house = houseDispatcher.edit(id, form);
        return ResponseEntity.ok(house);
    }

    // Удаление дома
    @DeleteMapping("house/{id}")
    public ResponseEntity<Void> deleteHouse(@PathVariable Long id) {
        houseDispatcher.delete(id);
        return ResponseEntity.ok().build();
    }

    // Создание новой задачи
    @PostMapping("task")
    public ResponseEntity<Task> createTask(@RequestBody Task.CreationBody body, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);

        try {
            // Создаём задачу в базе данных
            Task createdTask = taskDispatcher.createTask(body, employee);

            Set<Employee> observers = createdTask.getAllEmployeesObservers(employee);
            // Создаем оповещение о новой задаче
            notificationDispatcher.createNotification(observers, Notification.taskCreated(createdTask));

            // Отправляем сигнал пользователям, что задача создана
            stompController.createTask(createdTask);

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
    @DeleteMapping("task/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.deleteTask(id, employee);
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            stompController.updateTask(task);
            notificationDispatcher.createNotification(observers, Notification.taskDeleted(task, employee));
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
    @PostMapping("tasks/{page}")
    public ResponseEntity<Page<TaskListDto>> getTasks(@PathVariable Integer page, @Nullable @RequestBody TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
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

    @GetMapping("task/{id}/type-list/available-to-change")
    public ResponseEntity<List<TaskStage>> getAvailableTaskTypesToChange(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getAvailableTaskTypesToChange(id));
    }

    @GetMapping("task/{id}/directory-list/available-to-change")
    public ResponseEntity<List<TaskTypeDirectory>> getAvailableDirectoriesToChange(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getAvailableTaskDirectoryToChange(id));
    }

    // Получает страницу с задачами принадлежащими текущему наблюдателю
    @GetMapping("tasks/incoming/{page}")
    public ResponseEntity<Page<Task>> getIncomingTasks(@PathVariable Integer page, TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        condition.clean();
        return ResponseEntity.ok(taskDispatcher.getIncomingTasks(page, condition, employee));
    }

    @GetMapping("task/{taskId}/check-compatibility/{otTaskId}")
    public ResponseEntity<Map<String,Object>> checkCompatibility(@PathVariable Long taskId, @PathVariable Long otTaskId, HttpServletRequest  request) {
        Employee employee = getEmployeeFromRequest(request);
        try{
            taskDispatcher.checkCompatibility(taskId, otTaskId, employee);
            return ResponseEntity.ok(null);
        }catch (ResponseException e){
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("task/{taskId}/connect-to/{otTaskId}")
    public ResponseEntity<Void> connectToOldTracker(@PathVariable Long taskId, @PathVariable Long otTaskId, HttpServletRequest  request) {
        Employee employee = getEmployeeFromRequest(request);
        taskDispatcher.connectToOldTracker(taskId, otTaskId, employee);
        return ResponseEntity.ok().build();
    }

    //changeTaskStageInOldTracker(taskId: number, taskStageId: number) {
    //        return this.sendPatch(`api/private/task/${taskId}/old-tracker-stage/${taskStageId}/change`, {});
    //    }
    @PatchMapping("task/{taskId}/old-tracker-stage/{taskStageId}/change")
    public ResponseEntity<Void> changeTaskStageInOldTracker(@PathVariable Long taskId, @PathVariable Integer taskStageId, HttpServletRequest  request) {
        Employee employee = getEmployeeFromRequest(request);
        taskDispatcher.changeTaskStageInOldTracker(taskId, taskStageId, employee);
        return ResponseEntity.ok().build();
    }

    @GetMapping("wireframe/{id}/filter-fields")
    public ResponseEntity<List<FilterModelItem>> getFiltrationFields(@PathVariable Long id){
        return ResponseEntity.ok(wireframeDispatcher.getFiltrationFields(id));
    }

    @GetMapping("tasks/by-login/{login}")
    public ResponseEntity<Page<Task>> getTasksByLogin(@PathVariable String login, @RequestParam Integer page) {
        return ResponseEntity.ok(taskDispatcher.getTasksByLogin(login, page, 5));
    }

    @GetMapping("wireframe/{id}/stages")
    public ResponseEntity<List<TaskStage>> getStages(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        Wireframe wireframe = wireframeDispatcher.getWireframeById(id);
        if(wireframe == null) throw new EntryNotFound("Шаблон не найден");
        List<TaskStage> stages = wireframe.getStages();
        taskDispatcher.getIncomingTasksCount(employee, id);
        if(stages == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(stages);
    }

    @GetMapping("wireframe/{id}/dashboard-statistic")
    public ResponseEntity<TaskDispatcher.WireframeDashboardStatistic> getWireframeDashboardStatistic(@PathVariable Long id) {
        return ResponseEntity.ok(taskDispatcher.getWireframeDashboardStatistic(id));
    }

    // Получает количество задач принадлежащих текущему наблюдателю
    @GetMapping("tasks/incoming/count")
    public ResponseEntity<Long> getCountIncomingTasks(HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCount(employee));
    }

    // Получает количество задач принадлежащих текущему наблюдателю отфильтрованы по шаблонам
    @GetMapping("tasks/incoming/wireframe/{wireframeId}/count")
    public ResponseEntity<Long> getCountIncomingTasksWireframe(@PathVariable Long wireframeId, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCount(employee, wireframeId));
    }

    @GetMapping("tasks/wireframe/by-tags/count")
    public ResponseEntity<Map<Long,Long>> getCountTasksWireframeByTag(@RequestParam List<Long> wireframeIds) {
        return ResponseEntity.ok(taskDispatcher.getTasksCountByTags(wireframeIds));
    }

    @GetMapping("tasks/incoming/wireframe/by-tags/count")
    public ResponseEntity<Map<Long,Long>> getCountIncomingTasksWireframeByTag(@RequestParam List<Long> wireframeIds, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCountByTags(employee, wireframeIds));
    }

    @GetMapping("tasks/incoming/wireframe/{wireframeId}/by-stages/count")
    public ResponseEntity<Map<String,Long>> getCountIncomingTasksByStages(@PathVariable Long wireframeId, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getIncomingTasksCountByStages(employee, wireframeId));
    }

    @GetMapping("tasks/wireframe/{wireframeId}/by-stages/count")
    public ResponseEntity<Map<String,Long>> getCountTasksByStages(@PathVariable Long wireframeId) {
        return ResponseEntity.ok(taskDispatcher.getTasksCountByStages(wireframeId));
    }

    @PostMapping("tasks/count")
    public ResponseEntity<Long> getCountTasks(@RequestBody TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
//        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getTasksCount(condition));
    }

    @PostMapping("tags/catalog/list")
    public ResponseEntity<List<TagWithTaskCountItem>> getTagsListFromCatalog(@RequestBody TaskDispatcher.FiltrationConditions condition, HttpServletRequest request){
//       Employee employee = getEmployeeFromRequest(request);
       return ResponseEntity.ok(taskDispatcher.getTagsListFromCatalog(condition));
    }

    // Получает количество всех не закрытых задач по шаблонам
    @GetMapping("tasks/wireframe/{wireframeId}/count")
    public ResponseEntity<Long> getCountTasksWireframe(@PathVariable Long wireframeId) {
        return ResponseEntity.ok(taskDispatcher.getTasksCount(wireframeId));
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

            task.getAllEmployeesObservers().forEach(observer->{
                Map<Long, Map<Long, Long>> incomingTasksCountByTags = taskDispatcher.getIncomingTasksCountByTags(observer);
                stompController.updateIncomingTagTaskCounter(observer.getLogin(), incomingTasksCountByTags);
            });
            Map<Long, Map<Long, Long>> tasksCountByTags = taskDispatcher.getTasksCountByTags();
            stompController.updateTagTaskCounter(tasksCountByTags);

            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.changeTags(task, body, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Создает тег задачи
    @PostMapping("task-tag")
    public ResponseEntity<TaskTag> createTaskTag(@RequestBody TaskTag.Form form, HttpServletRequest request) {
        form.throwIfIncomplete();
        try {
            TaskTag taskTag = taskTagDispatcher.create(form, getEmployeeFromRequest(request));
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
    public ResponseEntity<TaskTag> updateTaskTag(@RequestBody TaskTag.Form form) {
        form.throwIfIncomplete();
        try {
            if(form.getId() == null) throw new ResponseException("Не установлен id тега в запросе");
            TaskTag modifyTag = taskTagDispatcher.modify(form);
            stompController.updateTaskTag(modifyTag);
            return ResponseEntity.ok(modifyTag);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Удаляет тег задачи
    @DeleteMapping("task-tag/{id}")
    public ResponseEntity<Void> deleteTaskTag(@PathVariable Long id) {
        try {
            stompController.deleteTaskTag(taskTagDispatcher.delete(id));
            return ResponseEntity.ok().build();
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    @GetMapping("task-tag/{id}")
    public ResponseEntity<TaskTag> getTaskTag(@PathVariable Long id) {
        return ResponseEntity.ok(taskTagDispatcher.get(id));
    }

    // Получает список доступных тегов задачи
    @GetMapping("task-tags")
    public ResponseEntity<List<TaskTag>> getAllTaskTags(@RequestParam @Nullable String query,
                                                        @RequestParam @Nullable Boolean includingRemove) {
        return ResponseEntity.ok(taskTagDispatcher.getAll(query, includingRemove));
    }

    // Создает комментарий к задаче
    @PostMapping("comment")
    public ResponseEntity<Comment> createComment(@RequestBody CommentData body, HttpServletRequest request) {
        if (body.getTaskId() == null) throw new ResponseException("Идентификатор задачи не может быть пустым");
        Employee currentUser = getEmployeeFromRequest(request);
        try {
            Comment comment = commentDispatcher.create(body, currentUser);
            Set<Employee> taskObservers = comment.getParent().getAllEmployeesObservers(currentUser);
            Set<Employee> referredEmployees = employeeDispatcher.getValidEmployees(comment.getReferredLogins());
            notificationDispatcher.createNotification(referredEmployees, Notification.mentionedInTask(comment.getParent()));
            notificationDispatcher.createNotification(taskObservers, Notification.newComment(comment));
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
            stompController.updateComment(CommentMapper.toDto(comment), comment.getParent().getTaskId().toString());
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
            stompController.deleteComment(CommentMapper.toDto(comment), comment.getParent().getTaskId().toString());
            return ResponseEntity.ok(comment);
        } catch (EntryNotFound | NotOwner e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает страницу с комментариями и событиями в конкретной задаче
    @GetMapping("task/{id}/journal")
    public ResponseEntity<Page<TaskJournalItem>> getTaskJournal(@PathVariable Long id, @RequestParam Long offset, @RequestParam @Nullable TaskJournalSortingTypes sorting, @RequestParam Integer limit) {
        Page<Comment> comments = commentDispatcher.getComments(id, offset, limit, sorting).map(CommentMapper::fromDto);
        List<TaskJournalItem> commentItems = comments.getContent().stream().map(commentDto -> (TaskJournalItem) commentDto).collect(Collectors.toList());
        if (comments.getTotalElements() == 0L) {
            List<TaskEvent> taskEvents = taskEventDispatcher.getTaskEvents(id, sorting);
            // Concat events and comments
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).toList();
            commentItems.addAll(taskEventsItems);
            if(sorting != null) {
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
                if(sorting == TaskJournalSortingTypes.CREATE_DATE_ASC){
                    taskEvents = taskEventDispatcher.getTaskEventsTo(id, firstComment.getCreated(), sorting);
                }else{
                    taskEvents = taskEventDispatcher.getTaskEventsFrom(id, firstComment.getCreated(), sorting);
                }
            }
            // Concat events and comments and sort by creation date
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).toList();
            commentItems.addAll(taskEventsItems);
            if(sorting != null) {
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
                if(sorting == TaskJournalSortingTypes.CREATE_DATE_ASC){
                    taskEvents = taskEventDispatcher.getTaskEventsFrom(id, firstComment.getCreated(), sorting);
                }else{
                    taskEvents = taskEventDispatcher.getTaskEventsTo(id, lastComment.getCreated(), sorting);
                }
            } else if (offset > 0) {
                taskEvents = taskEventDispatcher.getTaskEvents(id, firstComment.getCreated(), lastComment.getCreated(), sorting);
            }
            // Concat events and comments and sort by creation date
            List<TaskJournalItem> taskEventsItems = taskEvents.stream().map(taskEvent -> (TaskJournalItem) taskEvent).toList();
            commentItems.addAll(taskEventsItems);
            if(sorting != null) {
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
    @PatchMapping("task/{id}/observers")
    public ResponseEntity<Task> changeTaskObservers(@PathVariable Long id, @RequestBody ChangeTaskObserversDTO body, HttpServletRequest request) {
        try {
            Employee employeeFromRequest = getEmployeeFromRequest(request);
            Task previousTask = taskDispatcher.getTask(id);
            Set<Employee> previousObservers = new HashSet<>(previousTask.getAllEmployeesObservers());

            Task task = taskDispatcher.changeTaskObservers(id, body.getDepartmentObservers(), body.getPersonalObservers());
            Set<Employee> newObservers = new HashSet<>(task.getAllEmployeesObservers(employeeFromRequest));

            // Получаем Set новых наблюдателей из разницы newObservers и previousObserver
            Set<Employee> employeesObservers = new HashSet<>(newObservers);
            employeesObservers.removeAll(previousObservers);
            employeesObservers.remove(employeeFromRequest);

            notificationDispatcher.createNotification(employeesObservers, Notification.youObserver(task));

            stompController.updateTask(task);

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
    public ResponseEntity<Page<CommentDto>> getComments(@RequestParam Long taskId, @RequestParam Long offset, @RequestParam Integer limit, @Nullable @RequestParam TaskJournalSortingTypes sorting) {
        return ResponseEntity.ok(commentDispatcher.getComments(taskId, offset, limit, sorting));
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
    public void getAttachmentFile(@PathVariable String id,
                                  @RequestHeader(value = "Range", required = false) String rangeHeader,
                                  HttpServletResponse response) {
        // Пытаемся найти вложение в базе данных по его id
        Attachment attachments;
        try {
            attachments = attachmentDispatcher.getAttachments(id);
        } catch (EntryNotFound e) {
            throw new ResponseException("Файл не найден");
        }

        try {
            OutputStream os = response.getOutputStream();

            // Получаем размер фала
            long fileSize = Files.size(Path.of(attachments.getPath()));

            byte[] buffer = new byte[1024];

            try (RandomAccessFile file = new RandomAccessFile(attachments.getPath(), "r")) {
                if (rangeHeader == null) {
                    response.setHeader("Content-Type", attachments.getMimeType());
                    response.setHeader("Content-Length", String.valueOf(fileSize));
                    response.setStatus(HttpStatus.OK.value());
                    long pos = 0;
                    file.seek(pos);
                    while (pos < fileSize - 1) {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                    return;
                }

                String[] ranges = rangeHeader.split("-");
                long rangeStart = Long.parseLong(ranges[0].substring(6));
                long rangeEnd;
                if (ranges.length > 1) {
                    rangeEnd = Long.parseLong(ranges[1]);
                } else {
                    rangeEnd = fileSize - 1;
                }
                if (fileSize < rangeEnd) {
                    rangeEnd = fileSize - 1;
                }

                String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
                response.setHeader("Content-Type", attachments.getMimeType());
                response.setHeader("Content-Length", contentLength);
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                long pos = rangeStart;
                file.seek(pos);
                while (pos < rangeEnd) {
                    file.read(buffer);
                    os.write(buffer);
                    pos += buffer.length;
                }
                os.flush();


            } catch (FileNotFoundException e) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }

        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // Получает миниатюру
    @GetMapping("thumbnail/{id}")
    public void getAttachmentThumbnail(@PathVariable String id, HttpServletResponse response) {
        // Пытаемся найти вложение в базе данных по его id
        Attachment attachments;
        try {
            attachments = attachmentDispatcher.getAttachments(id);
        } catch (EntryNotFound e) {
            throw new ResponseException("Файл не найден");
        }

        if (attachments.getType() == FileType.PHOTO || attachments.getType() == FileType.VIDEO) {
            if (attachments.getThumbnail() == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }
            Path filePath = Path.of(attachments.getThumbnail());
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                long size = Files.size(filePath);
                response.setHeader("Content-Type", "image/jpeg");
                response.setHeader("Content-Length", String.valueOf(size));
                response.setStatus(HttpStatus.OK.value());
                inputStream.transferTo(response.getOutputStream());
            } catch (IOException e) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }
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
    public ResponseEntity<Void> assignInstallers(@PathVariable Long taskId, @RequestBody WorkLog.AssignBody body, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            WorkLog workLog = taskDispatcher.assignInstallers(taskId, body, employee);
            List<Employee> acceptedEmployees = workLogDispatcher.getAcceptedEmployees(workLog.getEmployees());
            telegramController.assignInstallers(workLog, employee, acceptedEmployees);
            stompController.updateTask(workLog.getTask());
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
    @PostMapping("task/{taskId}/force-close-work-log")
    public ResponseEntity<Void> forceCloseWorkLog(@PathVariable Long taskId, @RequestBody String reasonOfClosing, HttpServletRequest request) {
        try {
            WorkLog taskWorkPair = taskDispatcher.forceCloseWorkLog(taskId, reasonOfClosing, getEmployeeFromRequest(request));
            stompController.updateTask(taskWorkPair.getTask());
            stompController.closeChat(taskWorkPair.getChat());
            stompController.closeWorkLog(taskWorkPair);

            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.forceCloseWorkLog(taskWorkPair.getTask(), taskWorkPair, getEmployeeFromRequest(request)));
            stompController.createTaskEvent(taskId, taskEvent);

        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("task/move-to-directory")
    public ResponseEntity<Void> moveTaskToDirectory(@RequestBody TaskDispatcher.MovingToDirectoryForm form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        List<Task> tasks = taskDispatcher.moveToDirectory(form);
        for (Task task : tasks) {
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskMovedToDirectory(task, employee));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.movedToDirectory(task, employee));
            stompController.createTaskEvent(task.getTaskId(), taskEvent);
            stompController.updateTask(task);
        }
        return ResponseEntity.ok().build();
    }


    // Закрывает задачу
    @PatchMapping("task/{taskId}/close")
    public ResponseEntity<Task> closeTask(@PathVariable Long taskId, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.close(taskId, employee);
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskClosed(task, employee));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.close(task, employee));
            stompController.createTaskEvent(taskId, taskEvent);
            return ResponseEntity.ok(task);
        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Вновь открывает задачу
    @PatchMapping("task/{taskId}/reopen")
    public ResponseEntity<Task> reopenTask(@PathVariable Long taskId, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        try {
            Task task = taskDispatcher.reopen(taskId, employee);
            stompController.updateTask(task);
            Set<Employee> observers = task.getAllEmployeesObservers(employee);
            notificationDispatcher.createNotification(observers, Notification.taskReopened(task, employee));
            TaskEvent taskEvent = taskEventDispatcher.appendEvent(TaskEvent.reopen(task, employee));
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
            Employee employee = getEmployeeFromRequest(request);
            TaskFieldsSnapshotDispatcher.SnapshotBuilder snapshotBuilder = taskFieldsSnapshotDispatcher.builder().beforeEditing(taskId, employee);
            Task task = taskDispatcher.edit(taskId, modelItems, employee);
            snapshotBuilder.afterEditing().flush();
            stompController.updateTask(task);
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
        Department department = departmentsDispatcher.create(body.get("name"), body.get("description"));
        stompController.createDepartment(department);
        return ResponseEntity.ok(department);
    }

    // Редактирует отдел
    @PatchMapping("department/{id}")
    public ResponseEntity<Department> editDepartment(@RequestBody Map<String, String> body, @PathVariable Long id) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания отдела");
        if (body.get("name") == null || body.get("name").isBlank())
            throw new ResponseException("В запросе нет имени отдела");
        try {
            Department department = departmentsDispatcher.edit(id, body.get("name"), body.get("description"));
            stompController.updateDepartment(department);
            return ResponseEntity.ok(department);
        } catch (EntryNotFound e) {
            throw new ResponseException("Отдел с идентификатором " + id + " не найден в базе данных");
        }
    }

    // Удаляет отдел
    @DeleteMapping("department/{id}")
    public ResponseEntity<Department> deleteDepartment(@PathVariable Long id) {
        try {
            Department department = departmentsDispatcher.delete(id);
            stompController.deleteDepartment(department);
            return ResponseEntity.ok(department);
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
        Position position = positionDispatcher.create(body.getName(), body.getDescription(), body.getAccess());
        stompController.createPosition(position);
        return ResponseEntity.ok(position);
    }

    // Редактирует должность
    @PatchMapping("position/{id}")
    public ResponseEntity<Position> editPosition(@RequestBody PositionForm body, @PathVariable Long id) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания должности");
        if (body.getName() == null || body.getName().isBlank())
            throw new ResponseException("В запросе нет названия должности");
        try {
            Position position = positionDispatcher.edit(id, body.getName(), body.getDescription(), body.getAccess());
            stompController.updatePosition(position);
            return ResponseEntity.ok(position);
        } catch (EntryNotFound e) {
            throw new ResponseException("Должность с идентификатором " + id + " не найдена в базе данных");
        }
    }

    // Удаляет должность
    @DeleteMapping("position/{id}")
    public ResponseEntity<Position> deletePosition(@PathVariable Long id) {
        try {
            Position position = positionDispatcher.delete(id);
            stompController.deletePosition(position);
            return ResponseEntity.ok(position);
        } catch (EntryNotFound e) {
            throw new ResponseException("Должность с идентификатором " + id + " не найдена в базе данных");
        }
    }

    // Получает сотрудника по логину
    @GetMapping("employee/{login}")
    public ResponseEntity<Employee> getEmployee(@PathVariable String login) {
        Employee employeeByLogin;
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
            Employee employee = employeeDispatcher.create(body);
            stompController.createEmployee(employee);
            return ResponseEntity.ok(employee);
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
            Employee employee = employeeDispatcher.edit(body);
            stompController.updateEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException("Сотрудник с логином " + login + " не найден в базе данных для редактирования");
        } catch (EditingNotPossible e) {
            throw new ResponseException("Сотрудник удален, не возможно отредактировать");
        }
    }

    @PatchMapping("employee/phy-phone/null/bind")
    public ResponseEntity<Void> setPhyPhoneBind(HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        employeeDispatcher.setPhyPhoneBind(null, currentUser);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("employee/phy-phone/{phoneId}/bind")
    public ResponseEntity<Void> setPhyPhoneBind(@PathVariable Long phoneId, HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        employeeDispatcher.setPhyPhoneBind(phyPhoneService.get(phoneId), currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("phy-phone-list")
    public ResponseEntity<List<ListItem>> getPhyPhoneBind(HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        return ResponseEntity.ok(phyPhoneService.getPhyPhoneList());
    }

    @PostMapping("call-to-phone")
    public ResponseEntity<Void> callUp(@RequestBody PhyPhoneService.CallUpRequest callUpRequest, HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        if(currentUser.getPhyPhoneInfo() == null) throw new ResponseException("К аккаунту не привязан телефон");
        phyPhoneService.callUp(currentUser.getPhyPhoneInfo(), callUpRequest);
        return ResponseEntity.ok().build();
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
            stompController.deleteEmployee(employee);
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
        Path filePath = Path.of("./attachments", "avatars", file);
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(Files.probeContentType(filePath))).contentLength(bytes.length).body(bytes);
        } catch (IOException e) {
            throw new ResponseException("Не удалось прочитать файл");
        }
    }

    // Устанавливает аватар
    @PostMapping("employee/avatar")
    public ResponseEntity<Employee> setAvatar(@RequestBody String body, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);

        byte[] imageBytes = Base64.getDecoder().decode(body.replaceAll("data:[^/]+/[^;]+;base64,", "").getBytes(StandardCharsets.UTF_8));
        String fileName = UUID.randomUUID() + ".png";
        Path folderPath = Path.of("./attachments", "avatars");
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            throw new ResponseException("Не удалось прочитать файл");
        }

        try {
            Files.createDirectory(folderPath);
        } catch (IOException ignore) {
        }

        try {
            BufferedImage containerImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            containerImage.createGraphics().drawImage(image, 0, 0, Color.white, null);
            BufferedImage resized = Scalr.resize(containerImage, 250);
            Path filePath = folderPath.resolve(fileName);
            ImageIO.write(resized, "png", filePath.toFile());
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
//    @PostMapping("chat/{chatId}/message")
//    public ResponseEntity<Void> sendMessage(@PathVariable Long chatId, @RequestBody MessageData message, HttpServletRequest request) {
//        Employee employee = getEmployeeFromRequest(request);
//        try {
//            telegramController.sendMessageFromWeb(chatId, message, employee);
//            return ResponseEntity.ok().build();
//        } catch (EntryNotFound | EmptyFile | WriteError | TelegramApiException | IllegalFields | IllegalMediaType e) {
//            throw new ResponseException(e.getMessage());
//        }
//    }

    // Редактирует сообщение в чате
//    @PatchMapping("chat/message/{messageId}")
//    public ResponseEntity<Void> editMessage(@PathVariable Long messageId, @RequestBody String text, HttpServletRequest request) {
//        Employee employee = getEmployeeFromRequest(request);
//        try {
//            telegramController.updateMessageFromWeb(messageId, text, employee);
//        } catch (TelegramApiException | EntryNotFound | NotOwner | IllegalFields e) {
//            throw new ResponseException(e.getMessage());
//        }
//        return ResponseEntity.ok().build();
//    }

    // Удаляет сообщение из чата
//    @DeleteMapping("chat/message/{messageId}")
//    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId, HttpServletRequest request) {
//        Employee employee = getEmployeeFromRequest(request);
//        try {
//            telegramController.deleteMessageFromWeb(messageId, employee);
//            return ResponseEntity.ok().build();
//        } catch (EntryNotFound | NotOwner | AlreadyDeleted | TelegramApiException e) {
//            throw new ResponseException(e.getMessage());
//        }
//    }

    @PostMapping("chat/{chatId}/message/{superMessageId}/attach-to-task")
    public ResponseEntity<Void> attachToTask(@PathVariable Long superMessageId, @PathVariable Long chatId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        List<Attachment> attachments = chatDispatcher.getAttachments(superMessageId);
        commentDispatcher.attach(chatId, attachments, body.get("description"), employee);
        return ResponseEntity.ok().build();
    }

    // Помечает сообщения как прочитанные
    @PatchMapping("chat/messages/read")
    public ResponseEntity<Void> setMessagesAsRead(@RequestBody Set<Long> messageIds, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        SuperMessage chatMessages = chatDispatcher.setMessagesAsRead(messageIds, employee);
        if (chatMessages == null) return ResponseEntity.ok().build();
        stompController.updateCountUnreadMessage(employee.getLogin(), chatMessages.getParentChatId(),
                chatDispatcher.getUnreadMessagesCount(chatMessages.getParentChatId(), employee));
        stompController.updateMessage(chatMessages);
        return ResponseEntity.ok().build();
    }

    // Получает количество не прочитанных сообщений в чате
    @GetMapping("chat/{chatId}/messages/unread-count")
    public ResponseEntity<Long> getUnreadMessagesCount(@PathVariable Long chatId, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(chatDispatcher.getUnreadMessagesCount(chatId, employee));
    }

    // Получает страницу с сообщениями из чата
    @GetMapping("chat/{chatId}/messages")
    public ResponseEntity<Page<SuperMessage>> getChatMessages(@PathVariable Long chatId, @RequestParam Long first, @RequestParam Integer limit) {
        try {
            return ResponseEntity.ok(chatDispatcher.getChatMessages(chatId, first, limit));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает чат по chatId
    @GetMapping("chat/{chatId}")
    public ResponseEntity<Chat> getChat(@PathVariable Long chatId) {
        try {
            return ResponseEntity.ok(chatDispatcher.getChat(chatId));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получает список активных чатов сотрудника
    @GetMapping("chats/my/active")
    public ResponseEntity<List<Chat>> getMyActiveChats(HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(chatDispatcher.getMyActiveChats(employee));
    }

    // Получает активный чат задачи по taskId
    @GetMapping("task/{taskId}/active-chat")
    public ResponseEntity<Chat> getActiveChat(@PathVariable Long taskId) {
        try {
            WorkLog activeWorkLog = workLogDispatcher.getActiveWorkLogByTaskId(taskId);
            return ResponseEntity.ok(activeWorkLog.getChat());
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Отправляет список адресов как подсказки к автодополнению
    @GetMapping("suggestions/address")
    public ResponseEntity<List<AddressDto>> getAddressSuggestions(@RequestParam String query,
                                                                  @RequestParam @Nullable Boolean isAcpConnected,
                                                                  @RequestParam @Nullable Boolean isHouseOnly) {
        return ResponseEntity.ok(addressDispatcher.getSuggestions(query, isAcpConnected, isHouseOnly));
    }

    @GetMapping("suggestions/address/alt")
    public ResponseEntity<List<Address>> getAddressSuggestions(@RequestParam String query,
                                                                  @RequestParam Long streetId,
                                                                  @RequestParam @Nullable Boolean isAcpConnected,
                                                                  @RequestParam @Nullable Boolean isHouseOnly) {
        return ResponseEntity.ok(addressDispatcher.getSuggestions(streetId, query, isAcpConnected, isHouseOnly));
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

    // Получение страницы платных действий
    @GetMapping("salary/paid-actions/{page}")
    public ResponseEntity<Page<PaidAction>> getSalaryPaidActions(@PathVariable Integer page, @Nullable PaidAction.Filter filter) {
        return ResponseEntity.ok(paidActionDispatcher.getPage(page, filter));
    }

    // Получение доступных платных действий
    @GetMapping("salary/paid-actions/available")
    public ResponseEntity<List<PaidAction>> getAvailableSalaryPaidActions() {
        return ResponseEntity.ok(paidActionDispatcher.getAvailableList());
    }

    // Создание платных действий
    @PostMapping("salary/paid-action")
    public ResponseEntity<Void> createSalaryPaidAction(@RequestBody PaidAction.Form paidActionForm, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidActionDispatcher.create(paidActionForm, employee);
        return ResponseEntity.ok().build();
    }

    // Редактирование платных действий
    @PatchMapping("salary/paid-action/{id}")
    public ResponseEntity<Void> editSalaryPaidAction(@PathVariable Long id, @RequestBody PaidAction.Form paidActionForm, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidActionDispatcher.edit(id, paidActionForm, employee);
        return ResponseEntity.ok().build();
    }

    // Удаление платных действий
    @DeleteMapping("salary/paid-action/{id}")
    public ResponseEntity<Void> deleteSalaryPaidAction(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidActionDispatcher.delete(id, employee);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("salary/paid-works-tree/drag-drop")
    public ResponseEntity<Void> dragDropSalaryWorks(@RequestBody TreeDragDropEvent event, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        if (!event.hasSource()) throw new ResponseException("Нет источника");
        String sourceType = event.getSource().getType();
        if (sourceType.equals("group")) {
            paidWorkGroupDispatcher.dragDrop(event);
        } else if (sourceType.equals("work")) {
            paidWorkDispatcher.dragDrop(event);
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("salary/paid-works-tree/reposition")
    public ResponseEntity<Void> repositionSalaryWorks(@RequestBody List<TreeElementPosition> event, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        for (TreeElementPosition position : event) {
            switch (position.getType()) {
                case "group" -> position.setPath(paidWorkGroupDispatcher.reposition(position, employee));
                case "work" -> position.setPath(paidWorkDispatcher.reposition(position, employee));
            }
        }
        stompController.worksTreeReposition(event);
        return ResponseEntity.ok().build();
    }

    @GetMapping("salary/paid-works-tree/root")
    public ResponseEntity<List<TreeNode>> getSalaryPaidWorksTreeRoot(@RequestParam @Nullable Boolean groupsUndraggable) {
        return ResponseEntity.ok(paidWorkGroupDispatcher.getRootTree(groupsUndraggable));
    }

    @GetMapping("salary/paid-works-tree/{groupId}")
    public ResponseEntity<List<TreeNode>> getSalaryPaidWorksTree(@PathVariable Long groupId, @RequestParam @Nullable Boolean groupsUndraggable) {
        return ResponseEntity.ok(paidWorkGroupDispatcher.getTree(groupId, groupsUndraggable));
    }

    @PostMapping("salary/paid-work-group")
    public ResponseEntity<Void> createSalaryPaidWorkGroup(@RequestBody PaidWorkGroup.Form form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidWorkGroupDispatcher.create(form, employee);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("salary/paid-work-group/{id}")
    public ResponseEntity<Void> editSalaryPaidWorkGroup(@PathVariable Long id, @RequestBody PaidWorkGroup.Form form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidWorkGroupDispatcher.edit(id, form, employee);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("salary/paid-work-group/{id}")
    public ResponseEntity<Void> deleteSalaryPaidWorkGroup(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidWorkGroupDispatcher.delete(id, employee);
        return ResponseEntity.ok().build();
    }

    @PostMapping("salary/paid-work")
    public ResponseEntity<Void> createSalaryPaidWork(@RequestBody PaidWork.Form form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidWorkDispatcher.create(form, employee);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("salary/paid-work/{id}")
    public ResponseEntity<Void> editSalaryPaidWork(@PathVariable Long id, @RequestBody PaidWork.Form form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidWorkDispatcher.edit(id, form, employee);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("salary/paid-work/{id}")
    public ResponseEntity<Void> deleteSalaryPaidWork(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        paidWorkDispatcher.delete(id, employee);
        return ResponseEntity.ok().build();
    }

    @GetMapping("salary/paid-work/{id}")
    public ResponseEntity<PaidWork> getSalaryPaidWork(@PathVariable Long id) {
        return ResponseEntity.ok(paidWorkDispatcher.get(id));
    }

    @GetMapping("uncompleted-reports")
    public ResponseEntity<List<WorkLog>> getUncompletedReports(HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(workLogDispatcher.getUncompletedReports(employee));
    }

    //saveReport(form: {reportDescription: string, workLogId: number|null}) {
    //        return this.sendPatch("api/private/work-log/writing-report", form);
    //    }
    @PatchMapping("work-log/writing-report")
    public ResponseEntity<Void> saveReport(@RequestBody WorkLog.WritingReportForm form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        form.throwIfIncomplete();
        workLogDispatcher.saveReport(form, employee);
        return ResponseEntity.ok().build();
    }

    @PostMapping("salary/work-calculation")
    public ResponseEntity<Void> createSalaryWorkCalculation(@RequestBody WorkCalculationForm form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        workCalculationDispatcher.calculateAndSave(form, employee);
        return ResponseEntity.ok().build();
    }

    @PostMapping("salary/work-calculation/bypass")
    public ResponseEntity<Void> createBypassSalaryWorkCalculation(@RequestBody BypassWorkCalculationForm form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        workCalculationDispatcher.calculateAndSaveBypass(form, employee);
        return ResponseEntity.ok().build();
    }

    @GetMapping("salary/already-calculated-work/{id}/form")
    public ResponseEntity<ResponseWorkEstimationForm> getAlreadyCalculatedWorkForm(@PathVariable Long id) {
        return ResponseEntity.ok(workCalculationDispatcher.getFormInfoByWorkLog(id));
    }

    @GetMapping("salary/table")
    public ResponseEntity<SalaryTable> getSalaryTable(@RequestParam @Nullable Date date, @RequestParam @Nullable Long position) {
        return ResponseEntity.ok(workingDayDispatcher.getTableByDate(date, position));
    }

    @GetMapping("convert/billing-address-string")
    public ResponseEntity<Address> convertBillingAddressString(@RequestParam @Nullable String addressString) {
        if (addressString == null) return ResponseEntity.ok(null);
        return ResponseEntity.ok(addressDispatcher.convert(addressString));
    }

    @GetMapping("working-day")
    public ResponseEntity<WorkingDay> getWorkingDay(@RequestParam Date date, @RequestParam String login) {
        return ResponseEntity.ok(workingDayDispatcher.getWorkingDay(date, login));
    }

    @GetMapping("acp/dhcp/bindings")
    public ResponseEntity<List<DhcpBinding>> getDhcpBindings(@RequestParam String login) {
        return ResponseEntity.ok(acpClient.getBindingsByLogin(login));
    }

    @GetMapping("acp/dhcp/binding/{login}/logs/{page}")
    public ResponseEntity<LogsRequest> getDhcpBindingsLogs(@PathVariable String login, @PathVariable Integer page) {
        return ResponseEntity.ok(acpClient.getLogsByLogin(login, page));
    }

    @GetMapping("acp/vlan/{id}/dhcp/bindings/{page}")
    public ResponseEntity<Page<DhcpBinding>> getDhcpBindingsByVlan(@PathVariable Integer page, @PathVariable Integer id, @RequestParam String excludeLogin) {
        return ResponseEntity.ok(acpClient.getDhcpBindingsByVlan(page, id, excludeLogin));
    }

    @GetMapping("acp/dhcp/bindings/{page}/last")
    public ResponseEntity<Page<DhcpBinding>> getLastBindings(@PathVariable Integer page,
                                                             @RequestParam @Nullable Short state,
                                                             @RequestParam @Nullable String macaddr,
                                                             @RequestParam @Nullable String login,
                                                             @RequestParam @Nullable String ip,
                                                             @RequestParam @Nullable Integer vlan,
                                                             @RequestParam @Nullable Integer buildingId,
                                                             @RequestParam @Nullable Integer commutator,
                                                             @RequestParam @Nullable Integer port) {
        if(commutator == null)
            return ResponseEntity.ok(acpClient.getLastBindings(page, state, macaddr, login, ip, vlan, buildingId, null));

        return ResponseEntity.ok(acpClient.getLastBindings(page, state, macaddr, login, ip, vlan, buildingId, commutator, port));
    }

    @GetMapping("acp/dhcp/binding/{id}/ncl-history")
    public ResponseEntity<AcpClient.NCLHistoryWrapper> getNetworkConnectionLocation(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getNetworkConnectionLocationHistory(id));
    }

    @PostMapping("acp/dhcp/binding/auth")
    public ResponseEntity<Void> authDhcpBinding(@RequestBody DhcpBinding.AuthForm form) {
        apiBillingController.getUserInfo(form.getLogin());
        acpClient.authDhcpBinding(form);
        return ResponseEntity.ok().build();
    }

    @GetMapping("acp/buildings")
    public ResponseEntity<List<AcpHouse>> getBuildings(@Nullable @RequestParam String query) {
        return ResponseEntity.ok(acpClient.getHouses(query));
    }

    @GetMapping("acp/commutators/{page}/page")
    public ResponseEntity<Page<SwitchBaseInfo>> getCommutators(@PathVariable Integer page,
                                                               @RequestParam @Nullable String name,
                                                               @RequestParam @Nullable String ip,
                                                               @RequestParam @Nullable Integer buildingId) {
        return ResponseEntity.ok(acpClient.getCommutators(page, name, ip, buildingId, 15));
    }

    @GetMapping("acp/commutators/vlan/{vlan}")
    public ResponseEntity<List<Switch>> getCommutatorsByVlan(@PathVariable Integer vlan) {
        return ResponseEntity.ok(acpClient.getCommutatorsByVlan(vlan));
    }

    @GetMapping("acp/commutators/search")
    public ResponseEntity<List<SwitchWithAddress>> searchCommutators(@RequestParam @Nullable String query) {
        return ResponseEntity.ok(acpClient.searchCommutators(query));
    }

    @GetMapping("acp/commutator/{id}")
    public ResponseEntity<SwitchWithAddress> getCommutator(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutator(id));
    }

    @GetMapping("acp/commutator/{id}/editing-preset")
    public ResponseEntity<SwitchEditingPreset> getCommutatorEditingPreset(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutatorEditingPreset(id));
    }

    @PostMapping("acp/commutator/{id}/get-remote-update")
    public ResponseEntity<Void> getCommutatorRemoteUpdate(@PathVariable Integer id) {
        acpClient.getCommutatorRemoteUpdate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("acp/commutators/vlan/{vlan}/get-remote-update")
    public ResponseEntity<Void> getCommutatorsByVlanRemoteUpdate(@PathVariable Integer vlan) {
        acpClient.getCommutatorsByVlanRemoteUpdate(vlan);
        return ResponseEntity.ok().build();
    }

    @PostMapping("acp/commutators/get-remote-update")
    public ResponseEntity<Void> getCommutatorsRemoteUpdate() {
        acpClient.getAllCommutatorsRemoteUpdate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("acp/commutator/port/{id}/fdb")
    public ResponseEntity<List<FdbItem>> getCommutatorFdb(@PathVariable Long id) {
        return ResponseEntity.ok(acpClient.getFdbByPort(id));
    }


    @PostMapping("acp/commutator")
    public ResponseEntity<Void> createCommutator(@RequestBody Switch.Form form) {
        if (!form.isValid()) throw new IllegalFields("Неверно заполнена форма создания коммутатора");
        Switch createdCommutator = acpClient.createCommutator(form);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("acp/commutator/{id}")
    public ResponseEntity<Void> updateCommutator(@PathVariable Integer id, @RequestBody Switch.Form form) {
        if (!form.isValid()) throw new IllegalFields("Неверно заполнена форма редактирования коммутатора");
        Switch updatedCommutator = acpClient.updateCommutator(id, form);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("acp/commutator/{id}")
    public ResponseEntity<Void> deleteCommutator(@PathVariable Integer id) {
        acpClient.deleteCommutator(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("acp/commutator/check-exist/name")
    public ResponseEntity<Boolean> checkCommutatorNameExist(@RequestParam String name) {
        return ResponseEntity.ok(acpClient.checkCommutatorNameExist(name));
    }

    @GetMapping("acp/commutator/check-exist/ip")
    public ResponseEntity<Boolean> checkCommutatorIpExist(@RequestParam String ip) {
        return ResponseEntity.ok(acpClient.checkCommutatorIpExist(ip));
    }

    @GetMapping("acp/commutator/models")
    public ResponseEntity<List<SwitchModel>> getCommutatorModels(@RequestParam @Nullable String query) {
        return ResponseEntity.ok(acpClient.getCommutatorModels(query));
    }

    @GetMapping("acp/commutator/model/{id}")
    public ResponseEntity<SwitchModel> getCommutatorModel(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutatorModel(id));
    }

    @GetMapping("acp/building/{id}/address")
    public ResponseEntity<Address> getBuildingAddress(@PathVariable Integer id) {
        House houseByBind = houseDispatcher.getByAcpBindId(id);
        if (houseByBind == null) return ResponseEntity.ok(null);
        return ResponseEntity.ok(houseByBind.getAddress());
    }

    @GetMapping("remote-control/{ip}/check-access")
    public Mono<ResponseEntity<NetworkRemoteControl>> checkAccess(@PathVariable String ip) {
        if (ip == null) throw new IllegalFields("Не указан IP адрес");
        return NetworkRemoteControl.of(ip).map(ResponseEntity::ok);
    }

    @GetMapping("types/document-template")
    public ResponseEntity<List<Map<String, String>>> getDocumentTemplateTypes() {
        return ResponseEntity.ok(DocumentTemplate.getDocumentTypes());
    }

    @GetMapping("types/field-display")
    public ResponseEntity<List<Map<String, String>>> getFieldDisplayTypes() {
        return ResponseEntity.ok(FieldItem.DisplayType.getList());
    }

    @GetMapping("types/wireframe-field")
    public ResponseEntity<List<Map<String, String>>> getWireframeFieldTypes() {
        return ResponseEntity.ok(WireframeFieldType.getList());
    }

    @GetMapping("types/connection-service")
    public ResponseEntity<List<Map<String, String>>> getConnectionServiceTypes() {
        return ResponseEntity.ok(ConnectionService.getList());
    }

    @GetMapping("types/connection-type")
    public ResponseEntity<List<Map<String, String>>> getConnectionTypeTypes() {
        return ResponseEntity.ok(ConnectionType.getList());
    }

    @GetMapping("types/advertising-source")
    public ResponseEntity<List<Map<String, String>>> getAdvertisingSourceTypes() {
        return ResponseEntity.ok(AdvertisingSource.getList());
    }

    @GetMapping("types/billing-payment-type")
    public ResponseEntity<List<ListItem>> getPaymentTypeTypes() {
        return ResponseEntity.ok(BillingPayType.getList());
    }

    @GetMapping("types/files-sorting")
    public ResponseEntity<List<Map<String,String>>> getFilesSortingTypes() {
        return ResponseEntity.ok(FilesWatchService.FileSortingTypes.getList());
    }

    @GetMapping("types/phy-phone-models")
    public ResponseEntity<List<Map<String,String>>> getPhyPhoneModelsTypes() {
        return ResponseEntity.ok(PhyPhoneInfo.PhyPhoneModel.getList());
    }

    @GetMapping("types/connection-service/suggestions")
    public ResponseEntity<List<Map<String, String>>> getSuggestionForConnectionService(@Nullable @RequestParam String query) {
        return ResponseEntity.ok(
                ConnectionService.getList()
                        .stream()
                        .filter(service -> service.get("label").toLowerCase().contains(query != null ? query.toLowerCase() : ""))
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("configuration/telegram")
    public ResponseEntity<TelegramConf> getTelegramConfiguration() {
        return ResponseEntity.ok(telegramController.getConfiguration());
    }

    @PostMapping("configuration/telegram")
    public ResponseEntity<Void> updateTelegramConfiguration(@RequestBody TelegramConf conf) {
        try {
            telegramController.changeTelegramConf(conf);
            return ResponseEntity.ok().build();
        } catch (TelegramApiException e) {
            throw new ResponseException(e.getMessage());
        } catch (IOException e) {
            throw new TelegramBotNotInitialized(e.getMessage());
        }
    }

    @GetMapping("configuration/acp")
    public ResponseEntity<AcpConf> getAcpConfiguration() {
        return ResponseEntity.ok(acpClient.getConfiguration());
    }

    @PostMapping("configuration/acp")
    public ResponseEntity<Void> updateAcpConfiguration(@RequestBody AcpConf conf) {
        acpClient.setConfiguration(conf);
        return ResponseEntity.ok().build();
    }

    @GetMapping("client-equipments")
    public ResponseEntity<List<ClientEquipment>> getClientEquipment(@Nullable @RequestParam String query, @Nullable @RequestParam Boolean isDeleted) {
        return ResponseEntity.ok(clientEquipmentDispatcher.get(query, isDeleted));
    }

    @GetMapping("client-equipments/suggestions")
    public ResponseEntity<List<Map<String, String>>> getClientEquipment(@Nullable @RequestParam String query) {
        return ResponseEntity.ok(clientEquipmentDispatcher.get(query, false).stream()
                        .map(equipment -> {
                            return Map.of(
                                    "label", equipment.getName(),
                                    "value", equipment.getClientEquipmentId().toString()
                            );
                        })
                .collect(Collectors.toList()));
    }

    @PostMapping("client-equipment")
    public ResponseEntity<Void> createClientEquipment(@RequestBody ClientEquipment.Form form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        clientEquipmentDispatcher.create(form, employee);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("client-equipment/{id}")
    public ResponseEntity<Void> editClientEquipment(@PathVariable Long id, @RequestBody ClientEquipment.Form form, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        clientEquipmentDispatcher.edit(id, form, employee);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("client-equipment/{id}")
    public ResponseEntity<Void> deleteClientEquipment(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        clientEquipmentDispatcher.delete(id, employee);
        return ResponseEntity.ok().build();
    }

    @GetMapping("files/suggestions")
    public ResponseEntity<List<TFile.FileSuggestion>> getFilesSuggestions(@Nullable @RequestParam String query) {
        return ResponseEntity.ok(filesWatchService.getFileSuggestions(query));
    }

    @GetMapping("files/root")
    public ResponseEntity<List<FileSystemItem>> getRootFiles(@Nullable @RequestParam FilesWatchService.FileSortingTypes sortingType) {
        return ResponseEntity.ok(filesWatchService.getRoot(sortingType));
    }

    @GetMapping("files/directory/{id}")
    public ResponseEntity<FilesWatchService.LoadingDirectoryWrapper> getDirectory(@PathVariable Long id, @Nullable @RequestParam FilesWatchService.FileSortingTypes sortingType) {
        return ResponseEntity.ok(filesWatchService.getDirectory(id, sortingType));
    }

    @PatchMapping("files/move-to")
    public ResponseEntity<Void> filesMoveToDirectory(@RequestBody FilesWatchService.TransferEvent event, HttpServletRequest request) {
        filesWatchService.moveFiles(event.getSource(), event.getTarget());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("files/copy-to")
    public ResponseEntity<Void> filesCopyToDirectory(@RequestBody FilesWatchService.TransferEvent event, HttpServletRequest request) {
        filesWatchService.copyFiles(event.getSource(), event.getTarget());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("files/delete/{id}")
    public ResponseEntity<Void> filesDelete(@PathVariable Long id, HttpServletRequest request) {
        filesWatchService.deleteFile(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("files/rename")
    public ResponseEntity<Void> filesRename(@RequestBody FilesWatchService.RenameEvent event, HttpServletRequest request) {
        filesWatchService.renameFile(event.getId(), event.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("files/create-directory")
    public ResponseEntity<Void> filesCreateDirectory(@RequestBody FilesWatchService.CreateDirectoryEvent event, HttpServletRequest request) {
        filesWatchService.createDirectory(event.getParentDirectoryId(), event.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("files/load")
    public ResponseEntity<Void> filesLoad(@RequestBody List<FilesWatchService.LoadFileEvent> events, HttpServletRequest request) {
        for(FilesWatchService.LoadFileEvent event : events) {
            filesWatchService.loadFile(event.getName(), event.getData(), event.getTargetDirectoryId());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("files/search")
    public ResponseEntity<List<FileSystemItem>> searchFiles(@RequestParam String query, @Nullable @RequestParam FilesWatchService.FileSortingTypes sortingType) {
        return ResponseEntity.ok(filesWatchService.search(query, sortingType));
    }

    @GetMapping("file/{id}")
    public void getTFile(@PathVariable Long id,
                                  @RequestHeader(value = "Range", required = false) String rangeHeader,
                                  HttpServletResponse response) {
        TFile tFile = filesWatchService.getFileById(id).orElseThrow(()->new ResponseException("Файл не найден"));

        try {
            OutputStream os = response.getOutputStream();

            // Получаем размер фала
            long fileSize = Files.size(Path.of(tFile.getPath()));

            byte[] buffer = new byte[1024];

            try (RandomAccessFile file = new RandomAccessFile(tFile.getPath(), "r")) {
                if (rangeHeader == null) {
                    response.setHeader("Content-Type", tFile.getMimeType());
                    response.setHeader("Content-Length", String.valueOf(fileSize));
                    response.setHeader("Content-Disposition", "inline;filename="+tFile.getName());

                    response.setStatus(HttpStatus.OK.value());
                    long pos = 0;
                    file.seek(pos);
                    while (pos < fileSize - 1) {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                    return;
                }

                String[] ranges = rangeHeader.split("-");
                long rangeStart = Long.parseLong(ranges[0].substring(6));
                long rangeEnd;
                if (ranges.length > 1) {
                    rangeEnd = Long.parseLong(ranges[1]);
                } else {
                    rangeEnd = fileSize - 1;
                }
                if (fileSize < rangeEnd) {
                    rangeEnd = fileSize - 1;
                }

                String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
                response.setHeader("Content-Type", tFile.getMimeType());
                response.setHeader("Content-Length", contentLength);
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);
                response.setHeader("Content-Disposition", "inline;filename="+tFile.getName());
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                long pos = rangeStart;
                file.seek(pos);
                while (pos < rangeEnd) {
                    file.read(buffer);
                    os.write(buffer);
                    pos += buffer.length;
                }
                os.flush();


            } catch (FileNotFoundException e) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }

        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    @GetMapping("accounting/monthly-salary-report-table")
    public void getMonthlySalaryReportTable(@RequestParam Long date, HttpServletResponse response) {
        org.javatuples.Pair<Date,Date> monthBoundaries = Utils.getMonthBoundaries(new Date(date));
        Map<Date, List<WorkingDay>> workingDaysByOffsiteEmployees = workingDayDispatcher.getWorkingDaysByOffsiteEmployees(monthBoundaries.getValue0(), monthBoundaries.getValue1());
        MonthlySalaryReportTable document = TDocumentFactory.createMonthlySalaryReportTable(workingDaysByOffsiteEmployees, monthBoundaries.getValue0(), monthBoundaries.getValue1());
        document.sendByResponse(response);
    }

    @GetMapping("document-template")
    public void getConnectionAgreement(@RequestParam Long taskId, @RequestParam Long documentTemplateId, HttpServletResponse response) {
        taskDispatcher.getDocumentTemplate(taskId, documentTemplateId, response);
    }

    @GetMapping("ot/classes")
    public ResponseEntity<List<TaskClassOT>> getTaskClassesOT() {
        return ResponseEntity.ok(oldTrackerService.getTaskClasses());
    }

    private Employee getEmployeeFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) throw new ResponseException("Не авторизован");
        String login = AuthorizationProvider.getLoginFromCookie(List.of(request.getCookies()));
        if (login == null) throw new ResponseException("Не удалось получить данные о текущем пользователе");

        Employee currentUser;
        try {
            currentUser = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }

        if (currentUser.getDeleted()) throw new ResponseException("Сотрудник удален");

        return currentUser;
    }
}
