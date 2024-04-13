package com.microel.trackerbackend.services.api;

import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.*;
import com.microel.trackerbackend.misc.accounting.MonthlySalaryReportTable;
import com.microel.trackerbackend.misc.accounting.TDocumentFactory;
import com.microel.trackerbackend.misc.network.NetworkRemoteControl;
import com.microel.trackerbackend.misc.sorting.TaskJournalSortingTypes;
import com.microel.trackerbackend.parsers.addresses.AddressParser;
import com.microel.trackerbackend.parsers.oldtracker.AddressCorrectingPool;
import com.microel.trackerbackend.parsers.oldtracker.OldTracker;
import com.microel.trackerbackend.parsers.oldtracker.OldTrackerParserSettings;
import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.services.FilesWatchService;
import com.microel.trackerbackend.services.PhyPhoneService;
import com.microel.trackerbackend.services.external.billing.BillingPayType;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerService;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskClassOT;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.dto.mapper.CommentMapper;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.SuperMessage;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import com.microel.trackerbackend.storage.entities.comments.dto.CommentData;
import com.microel.trackerbackend.storage.entities.equipment.ClientEquipment;
import com.microel.trackerbackend.storage.entities.filesys.FileSystemItem;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.*;
import com.microel.trackerbackend.storage.entities.templating.*;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.data.domain.Page;
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
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("api/private")
public class PrivateRequestController {
    private final WireframeDispatcher wireframeDispatcher;
    private final TaskDispatcher taskDispatcher;
    private final StreetDispatcher streetDispatcher;
    private final HouseDispatcher houseDispatcher;
    private final CityDispatcher cityDispatcher;
    private final CommentDispatcher commentDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final AttachmentDispatcher attachmentDispatcher;
    private final DepartmentDispatcher departmentsDispatcher;
    private final PositionDispatcher positionDispatcher;
    private final StompController stompController;
    private final TaskTagDispatcher taskTagDispatcher;
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
    private final ClientEquipmentDispatcher clientEquipmentDispatcher;
    private final FilesWatchService filesWatchService;
    private final PhyPhoneService phyPhoneService;
    private final OldTrackerService oldTrackerService;

    public PrivateRequestController(WireframeDispatcher wireframeDispatcher, TaskDispatcher taskDispatcher,
                                    StreetDispatcher streetDispatcher, HouseDispatcher houseDispatcher, CityDispatcher cityDispatcher,
                                    CommentDispatcher commentDispatcher,
                                    EmployeeDispatcher employeeDispatcher, AttachmentDispatcher attachmentDispatcher,
                                    DepartmentDispatcher departmentsDispatcher, PositionDispatcher positionDispatcher,
                                    StompController stompController,
                                    TaskTagDispatcher taskTagDispatcher,
                                    NotificationDispatcher notificationDispatcher, WorkLogDispatcher workLogDispatcher,
                                    ChatDispatcher chatDispatcher, TelegramController telegramController,
                                    OldTracker oldTracker, AddressParser addressParser, AddressDispatcher addressDispatcher,
                                    PaidActionDispatcher paidActionDispatcher, PaidWorkGroupDispatcher paidWorkGroupDispatcher,
                                    PaidWorkDispatcher paidWorkDispatcher, WorkCalculationDispatcher workCalculationDispatcher,
                                    WorkingDayDispatcher workingDayDispatcher, ClientEquipmentDispatcher clientEquipmentDispatcher,
                                    FilesWatchService filesWatchService, PhyPhoneService phyPhoneService,
                                    OldTrackerService oldTrackerService) {
        this.wireframeDispatcher = wireframeDispatcher;
        this.taskDispatcher = taskDispatcher;
        this.streetDispatcher = streetDispatcher;
        this.houseDispatcher = houseDispatcher;
        this.cityDispatcher = cityDispatcher;
        this.commentDispatcher = commentDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.attachmentDispatcher = attachmentDispatcher;
        this.departmentsDispatcher = departmentsDispatcher;
        this.positionDispatcher = positionDispatcher;
        this.stompController = stompController;
        this.taskTagDispatcher = taskTagDispatcher;
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
        this.clientEquipmentDispatcher = clientEquipmentDispatcher;
        this.filesWatchService = filesWatchService;
        this.phyPhoneService = phyPhoneService;
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

    @PatchMapping("house/{id}/to-apartments-building")
    public ResponseEntity<Void> makeHouseAnApartmentsBuilding(@PathVariable Long id) {
        houseDispatcher.makeHouseAnApartmentsBuilding(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("wireframe/{id}/filter-fields")
    public ResponseEntity<List<FilterModelItem>> getFiltrationFields(@PathVariable Long id) {
        return ResponseEntity.ok(wireframeDispatcher.getFiltrationFields(id));
    }


    @GetMapping("wireframe/{id}/stages")
    public ResponseEntity<List<TaskStage>> getStages(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = getEmployeeFromRequest(request);
        Wireframe wireframe = wireframeDispatcher.getWireframeById(id);
        if (wireframe == null) throw new EntryNotFound("Шаблон не найден");
        List<TaskStage> stages = wireframe.getStages();
        taskDispatcher.getIncomingTasksCount(employee, id);
        if (stages == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(stages);
    }

    @GetMapping("wireframe/{id}/dashboard-statistic")
    public ResponseEntity<TaskDispatcher.WireframeDashboardStatistic> getWireframeDashboardStatistic(@PathVariable Long id) {
        return ResponseEntity.ok(taskDispatcher.getWireframeDashboardStatistic(id));
    }


    @PostMapping("tags/catalog/list")
    public ResponseEntity<List<TagWithTaskCountItem>> getTagsListFromCatalog(@RequestBody TaskDispatcher.FiltrationConditions condition, HttpServletRequest request) {
//       Employee employee = getEmployeeFromRequest(request);
        return ResponseEntity.ok(taskDispatcher.getTagsListFromCatalog(condition));
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
            if (form.getId() == null) throw new ResponseException("Не установлен id тега в запросе");
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

    // Получает страницу комментариев к задаче
    @GetMapping("comments")
    public ResponseEntity<Page<CommentDto>> getComments(@RequestParam Long taskId, @RequestParam Long offset, @RequestParam Integer limit, @Nullable @RequestParam TaskJournalSortingTypes sorting) {
        return ResponseEntity.ok(commentDispatcher.getComments(taskId, offset, limit, sorting));
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
            List<Employee> employees = employeeDispatcher.getEmployeesByPositionId(position.getPositionId());
            for (Employee employee : employees) {
                stompController.updateEmployee(employee);
            }
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

    @GetMapping("phy-phone-list")
    public ResponseEntity<List<ListItem>> getPhyPhoneBind(HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        return ResponseEntity.ok(phyPhoneService.getPhyPhoneList());
    }

    @PostMapping("call-to-phone")
    public ResponseEntity<Void> callUp(@RequestBody PhyPhoneService.CallUpRequest callUpRequest, HttpServletRequest request) {
        Employee currentUser = getEmployeeFromRequest(request);
        if (currentUser.getPhyPhoneInfo() == null) throw new ResponseException("К аккаунту не привязан телефон");
        phyPhoneService.callUp(currentUser.getPhyPhoneInfo(), callUpRequest);
        return ResponseEntity.ok().build();
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
    public ResponseEntity<List<Map<String, String>>> getFilesSortingTypes() {
        return ResponseEntity.ok(FilesWatchService.FileSortingTypes.getList());
    }

    @GetMapping("types/phy-phone-models")
    public ResponseEntity<List<Map<String, String>>> getPhyPhoneModelsTypes() {
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
        for (FilesWatchService.LoadFileEvent event : events) {
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
        TFile tFile = filesWatchService.getFileById(id).orElseThrow(() -> new ResponseException("Файл не найден"));

        try {
            OutputStream os = response.getOutputStream();

            // Получаем размер фала
            long fileSize = Files.size(Path.of(tFile.getPath()));

            byte[] buffer = new byte[1024];

            try (RandomAccessFile file = new RandomAccessFile(tFile.getPath(), "r")) {
                if (rangeHeader == null) {
                    response.setHeader("Content-Type", tFile.getMimeType());
                    response.setHeader("Content-Length", String.valueOf(fileSize));
                    response.setHeader("Content-Disposition", "inline;filename=" + tFile.getName());

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
                response.setHeader("Content-Disposition", "inline;filename=" + tFile.getName());
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

    @GetMapping("file-thumbnail/{id}")
    public void getTFileThumbnail(@PathVariable Long id,
                                  @RequestHeader(value = "Range", required = false) String rangeHeader,
                                  HttpServletResponse response) {
        TFile tFile = filesWatchService.getFileById(id).orElseThrow(() -> new ResponseException("Файл не найден"));

        try {
            OutputStream os = response.getOutputStream();

            // Получаем размер фала
            long fileSize = Files.size(Path.of(tFile.getThumbnail()));

            byte[] buffer = new byte[1024];

            try (RandomAccessFile file = new RandomAccessFile(tFile.getThumbnail(), "r")) {
                if (rangeHeader == null) {
                    response.setHeader("Content-Type", tFile.getMimeType());
                    response.setHeader("Content-Length", String.valueOf(fileSize));
                    response.setHeader("Content-Disposition", "inline;filename=" + tFile.getName());

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
                response.setHeader("Content-Disposition", "inline;filename=" + tFile.getName());
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
        org.javatuples.Pair<Date, Date> monthBoundaries = Utils.getMonthBoundaries(new Date(date));
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
