package com.microel.trackerbackend.parsers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.misc.CircularQueue;
import com.microel.trackerbackend.misc.SimpleMessage;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.mapper.AddressMapper;
import com.microel.trackerbackend.storage.dto.mapper.WireframeMapper;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.dto.templating.WireframeDto;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Getter
@Setter
@Slf4j
public class OldTracker {
    @JsonIgnore
    private final ConfigurationStorage configurationStorage;
    @JsonIgnore
    private final AddressDispatcher addressDispatcher;
    @JsonIgnore
    private final StreetDispatcher streetDispatcher;
    @JsonIgnore
    private final TaskDispatcher taskDispatcher;
    @JsonIgnore
    private final EmployeeDispatcher employeeDispatcher;
    @JsonIgnore
    private final StompController stompController;
    @JsonIgnore
    private final WireframeDispatcher wireframeDispatcher;
    @JsonIgnore
    private final Map<String, City> cityMap = new HashMap<>();
    private UncreatedTasksPool notCreatedTasksPool = new UncreatedTasksPool();
    private AddressCorrectingPool addressCorrectingPool = new AddressCorrectingPool();
    private OldTrackerParserSettings settings;
    private Map<String, String> cookies = new HashMap<>();
    private Boolean isRunning = false;
    private Integer lastParsedId = 0;
    @JsonIgnore
    private Instant startedTime = null;
    @Nullable
    private Integer currentTask = null;
    @JsonIgnore
    private CircularQueue<Long> averageTimeOfLastTasks = new CircularQueue<>(30);
    @JsonIgnore
    private List<WireframeDto> wireframeDtoList = new ArrayList<>();

    public OldTracker(ConfigurationStorage configurationStorage, AddressDispatcher addressDispatcher, StreetDispatcher streetDispatcher,
                      TaskDispatcher taskDispatcher, EmployeeDispatcher employeeDispatcher, CityDispatcher cityDispatcher, StompController stompController, WireframeDispatcher wireframeDispatcher) {
        this.configurationStorage = configurationStorage;
        this.addressDispatcher = addressDispatcher;
        this.streetDispatcher = streetDispatcher;
        this.taskDispatcher = taskDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
        this.wireframeDispatcher = wireframeDispatcher;
        cityDispatcher.getCities().forEach(city -> cityMap.put(city.getName(), city));
        this.settings = configurationStorage.loadOrDefault(OldTrackerParserSettings.class, new OldTrackerParserSettings());
        try {
            this.notCreatedTasksPool = configurationStorage.load(UncreatedTasksPool.class);
            this.addressCorrectingPool = configurationStorage.load(AddressCorrectingPool.class);
        } catch (FailedToReadConfigurationException ignored) {
        }
    }

    public void createTasksFromCorrectedAddresses(AddressCorrectingPool pool) {
        pool.forEach((key, value) -> {
            TaskDto uncreatedTask = notCreatedTasksPool.get(key);
            AddressCorrecting addressCorrecting = addressCorrectingPool.get(key);
            if (uncreatedTask != null && addressCorrecting != null) {
                uncreatedTask.getFields().stream()
                        .filter(modelItemDto -> modelItemDto.getWireframeFieldType().equals(WireframeFieldType.ADDRESS))
                        .findFirst().ifPresent(modelItemDto -> {
                            modelItemDto.setAddressData(AddressMapper.toDto(value.address));
                            notCreatedTasksPool.remove(key);
                            addressCorrectingPool.remove(key);
                            taskDispatcher.unsafeSave(uncreatedTask);
                            stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создана: " + uncreatedTask));
                        });
            }
        });
    }

    private DTO toDto() {
        return new DTO(settings, cookies, isRunning, lastParsedId, currentTask, getAverageTimePerTask(), getRemainingTime(), getElapsedTime());
    }

    public void setSettings(OldTrackerParserSettings settings) {
        this.settings = settings;
        saveSettings();
        stompController.updateTrackerParser(this.toDto());
    }

    public void startParse() throws IOException {
        final String authToken = getAuthToken();
        final Integer startId = settings.getStartId();
        final Integer endId = settings.getEndId();
        currentTask = startId;
        authorize();
        isRunning = true;
        wireframeDtoList = wireframeDispatcher.getAllWireframes(true).stream().map(WireframeMapper::toDto).collect(Collectors.toList());
        Executors.newSingleThreadExecutor().execute(() -> {
            startedTime = Instant.now();
            while (currentTask != null && currentTask <= endId && isRunning) {
                try {
                    final Instant startTime = Instant.now();
                    taskPageParse(currentTask, authToken);
                    averageTimeOfLastTasks.add(Duration.between(startTime, Instant.now()).toMillis());
                } catch (IOException ignore) {
                }
                currentTask++;
                stompController.updateTrackerParser(this.toDto());
            }
            isRunning = false;
            settings.setStartId(lastParsedId + 1);
            saveSettings();
            stompController.updateTrackerParser(this.toDto());
        });
    }

    public void stopParse() {
        isRunning = false;
        stompController.updateTrackerParser(this.toDto());
    }

    private void saveSettings() {
        try {
            configurationStorage.save(settings);
        } catch (FailedToWriteConfigurationException ignore) {
        }
    }

    public void taskPageParse(Integer id, String authToken) throws IOException {
        Document page = Jsoup
                .connect(settings.getTrackerUrl() + "/main.php?mode=obji_summary&obji=" + id + "&from_cat=1&log")
                .header("Authorization", authToken).cookies(cookies).execute().bufferUp().parse();
        String taskType = page.body().select("h1>span").text();
        if (taskType.isBlank()) {
//            stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.WARNING, "Нет задачи №" + id));
            return;
        }

        final String taskStageRegexp = "Этап: ([А-я\\s]+) Поля:";
        String taskStageName = page.body().text();
        final Matcher taskStageMatcher = Pattern.compile(taskStageRegexp).matcher(taskStageName);

        if (taskStageMatcher.find()) {
            taskStageName = taskStageMatcher.group(1);
        } else {
            return;
        }


        // Получаем общую для задач информацию
        String comments = page.body().select("div>i").html();
        String authorAndTimestamp = page.body().select("i").text();

        DateTimeFormatter creationTimestampFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm", Locale.ENGLISH);
        LocalDateTime creationTimestamp = LocalDateTime.parse(authorAndTimestamp.substring(0, 16), creationTimestampFormat);
        String year = String.valueOf(creationTimestamp.getYear());

        Employee author = new Employee();

        final String commentRegexp = ">(\\d{2}-[A-z]{3} \\d{2}:\\d{2}), ([^:]+):<\\/span>&nbsp;&nbsp;&nbsp;([^<]+)<br><br>";
        final String authorRegexp = "\\d{2}-\\d{2}-\\d{4} \\d{2}-\\d{2}, ([^ ]+)";

        final Matcher commentMatcher = Pattern.compile(commentRegexp).matcher(comments);
        final Matcher authorMatcher = Pattern.compile(authorRegexp).matcher(authorAndTimestamp);

        if (authorMatcher.find()) {
            author.setLogin(authorMatcher.group(1));
            author.setDeleted(true);
            if (employeeDispatcher.saveIsNotExist(author))
                stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создан: " + author));
        } else {
            stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.ERROR, "Создатель не найден: " + authorAndTimestamp));
            return;
        }


        List<Comment> commentsList = new ArrayList<>();

        while (commentMatcher.find()) {
            String stringTime = new StringBuffer(commentMatcher.group(1)).insert(6, "-" + year).toString();
            DateTimeFormatter commentTimestampFormant = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);
            LocalDateTime commentTime = LocalDateTime.parse(stringTime, commentTimestampFormant);
            Comment comment = new Comment();
            comment.setCreated(Timestamp.valueOf(commentTime));
            Employee commentEmployee = new Employee();
            commentEmployee.setLogin(commentMatcher.group(2));
            commentEmployee.setDeleted(true);
            comment.setCreator(commentEmployee);
            comment.setDeleted(false);
            comment.setEdited(false);
            comment.setAttachments(new ArrayList<>());
            comment.setMessage(commentMatcher.group(3));
            commentsList.add(comment);
            if (employeeDispatcher.saveIsNotExist(commentEmployee))
                stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создан: " + commentEmployee));
        }

        TaskFields taskFields = TaskFields.from(page, this);

        UUID uniqueTaskCreationID = UUID.randomUUID();

        OldTrackerTaskFactory taskFactory = new OldTrackerTaskFactory(settings.getBindings(), author, creationTimestamp, commentsList, wireframeDtoList, taskStageName);

        switch (taskType) {
            case "Аварии (Волгодонск)": {

                String login = taskFields.get(8).getText();
                String phone = taskFields.get(15).getPhone();
                String description = taskFields.get(18).getText();
                String workReport = taskFields.get(23).getText();
                Address address = null;
                try {
                    address = taskFields.getAddress(uniqueTaskCreationID, cityMap.get("Волгодонск"), 10, 11, null, null, 12);
                    TaskDto task = taskFactory.createAccident(login, address, phone, description, workReport);
                    taskDispatcher.unsafeSave(task);
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создана #" + id + ": " + task));
                } catch (Exception e) {
                    TaskDto uncreated = taskFactory.createAccident(login, null, phone, description, workReport);
                    notCreatedTasksPool.put(uniqueTaskCreationID, uncreated);
                    try {
                        configurationStorage.save(addressCorrectingPool);
                    } catch (FailedToWriteConfigurationException ex) {
                        log.error("Не удалось записать AddressCorrectingPool в файл");
                    }
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.WARNING, "Добавлена в список на исправление: " + uncreated));
                } finally {
                    lastParsedId = id;
                }
                break;
            }
            case "Подключения": {

                String takenFrom = taskFields.get(1).getText();
                String type = taskFields.get(2).getText();
                String advertisingSource = taskFields.get(7).getText();
                String lastName = taskFields.get(8).getText();
                String firstName = taskFields.get(9).getText();
                String middleName = taskFields.get(10).getText();
                StringBuilder fullName = new StringBuilder();
                if (lastName != null) fullName.append(lastName).append(" ");
                if (firstName != null) fullName.append(firstName).append(" ");
                if (middleName != null) fullName.append(middleName);
                String login = taskFields.get(27).getText();
                String password = taskFields.get(28).getText();
                String techWork = taskFields.get(32).getText();
                String phone = taskFields.get(16).getPhone();
                Address address = null;
                try {
                    address = taskFields.getAddress(uniqueTaskCreationID, cityMap.get("Волгодонск"), 11, 12, 14, 15, 13);
                    TaskDto task = taskFactory.createConnection(takenFrom, type, login, password, fullName.toString(), address, phone, advertisingSource, techWork);
                    taskDispatcher.unsafeSave(task);
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создана #" + id + ": " + task));
                } catch (Exception e) {
                    TaskDto uncreated = taskFactory.createConnection(takenFrom, type, login, password, fullName.toString(), null, phone, advertisingSource, techWork);
                    notCreatedTasksPool.put(uniqueTaskCreationID, uncreated);
                    try {
                        configurationStorage.save(addressCorrectingPool);
                    } catch (FailedToWriteConfigurationException ex) {
                        log.error("Не удалось записать AddressCorrectingPool в файл");
                    }
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.WARNING, "Добавлена в список на исправление: " + uncreated));
                } finally {
                    lastParsedId = id;
                }
                break;
            }
            case "ЧС Волгодонск": {

                String district = taskFields.get(1, 1).getText();
                String phone = taskFields.get(4, 1).getPhone();
                String name = taskFields.get(5, 1).getText();
                String takenFrom = taskFields.get(6, 1).getText();

                Address address = null;
                try {
                    address = taskFields.getAddress(uniqueTaskCreationID, cityMap.get("Волгодонск"), 2, 3, null, null, null, 1);
                    TaskDto task = taskFactory.createPrivateSectorVD(district, address, phone, name, takenFrom);
                    taskDispatcher.unsafeSave(task);
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создана #" + id + ": " + task));
                } catch (Exception e) {
                    TaskDto uncreated = taskFactory.createPrivateSectorVD(district, null, phone, name, takenFrom);
                    notCreatedTasksPool.put(uniqueTaskCreationID, uncreated);
                    try {
                        configurationStorage.save(addressCorrectingPool);
                    } catch (FailedToWriteConfigurationException ex) {
                        log.error("Не удалось записать AddressCorrectingPool в файл");
                    }
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.WARNING, "Добавлена в список на исправление: " + uncreated));
                } finally {
                    lastParsedId = id;
                }
                break;
            }
            case "ЧС Романовская (Микроэл)": {

                String gardening = taskFields.get(1, 1).getText();
                String name = taskFields.get(5, 1).getText();
                String phone = taskFields.get(4, 1).getPhone();
                String advertisingSource = taskFields.get(6, 1).getText();

                Address address = null;
                try {
                    address = taskFields.getAddress(uniqueTaskCreationID, cityMap.get("Романовская"), 2, 3, null, null, null, 1);
                    TaskDto task = taskFactory.createPrivateSectorRM(gardening, address, name, phone, advertisingSource);
                    taskDispatcher.unsafeSave(task);
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.INFO, "Создана #" + id + ": " + task));
                } catch (Exception e) {
                    TaskDto uncreated = taskFactory.createPrivateSectorRM(gardening, null, name, phone, advertisingSource);
                    notCreatedTasksPool.put(uniqueTaskCreationID, uncreated);
                    try {
                        configurationStorage.save(addressCorrectingPool);
                    } catch (FailedToWriteConfigurationException ex) {
                        log.error("Не удалось записать AddressCorrectingPool в файл");
                    }
                    stompController.sendParserMessage(new SimpleMessage(SimpleMessage.Severity.WARNING, "Добавлена в список на исправление: " + uncreated));
                } finally {
                    lastParsedId = id;
                }
                break;
            }
        }
    }

    @JsonIgnore
    private String getAuthToken() {
        return "Basic " + Base64.getEncoder().encodeToString((settings.getTrackerLogin() + ":" + settings.getTrackerPassword()).getBytes());
    }

    public Double getAverageTimePerTask() {
        if (averageTimeOfLastTasks.size() == 0) return null;
        OptionalDouble average = averageTimeOfLastTasks.stream().mapToLong(Long::longValue).average();
        if (average.isPresent()) {
            return average.getAsDouble();
        }
        return 0.0d;
    }

    public Double getRemainingTime() {
        Double averageTimePerTask = getAverageTimePerTask();
        if (averageTimePerTask == null || currentTask == null) return 0.0d;
        return (settings.getEndId() - currentTask) * averageTimePerTask;
    }

    public Long getElapsedTime() {
        if (startedTime == null) return 0L;
        return Duration.between(startedTime, Instant.now()).toMillis();
    }

    private void authorize() throws IOException {
        Connection.Response authorization = Jsoup
                .connect(settings.getTrackerUrl() + "/main.php?mode=list_objis").ignoreHttpErrors(true)
                .execute();
        cookies = authorization.cookies();
    }

    public enum AddressCorrectingType {
        STREET("STREET"), HOUSE("HOUSE"), APART("APART");
        private final String name;

        AddressCorrectingType(String name) {
            this.name = name;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DTO {
        private OldTrackerParserSettings settings;
        private Map<String, String> cookies = new HashMap<>();
        private Boolean isRunning = false;
        private Integer lastParsedId = 0;
        private Integer currentTask = null;
        private Double averageTimePerTask = 0.0d;
        private Double remainingTime = 0.0d;
        private Long elapsedTime = 0L;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressCorrecting {
        @NonNull
        private Address address;
        private String streetRaw;
        private String houseRaw;
        private String apartRaw;
        private Set<AddressCorrectingType> types = new HashSet<>();
    }

    public static class TaskField {
        @Nullable
        private String value;

        public TaskField(@Nullable String value) {
            this.value = value;
        }

        public String getText() {
            if (value == null) return null;
            if (value.equals("[пусто]")) return null;
            return value.trim();
        }

        public String getPhone() {
            if (value == null) return null;
            if (value.equals("[пусто]")) return null;
            value = value.trim().replace("[^\\d]", "");
            final String phoneRegex = "9\\d{9}";
            Matcher phoneMatcher = Pattern.compile(phoneRegex).matcher(value);
            if (phoneMatcher.find()) {
                value = phoneMatcher.group();
                return new StringBuffer(value).insert(8, "-").insert(6, "-").insert(3, ") ").insert(0, "8 (").toString();
            }
            return null;
        }
    }

    public static class TaskFields {
        private final Document page;
        private final OldTracker context;

        public TaskFields(Document page, OldTracker tracker) {
            this.page = page;
            this.context = tracker;
        }

        public static TaskFields from(Document page, OldTracker tracker) {
            return new TaskFields(page, tracker);
        }

        public TaskField get(@Nullable Integer rowIndex, @Nullable Integer tableIndex) {
            if (tableIndex == null) tableIndex = 2;
            if (rowIndex == null) return new TaskField(null);
            return new TaskField(page.body().select("body>table:nth-of-type(" + tableIndex + ")>tbody>tr:nth-of-type(" + rowIndex + ")>td:nth-of-type(2)").text());
        }

        public TaskField get(Integer rowIndex) {
            return get(rowIndex, null);
        }

        public Address getAddress(UUID tempId, City city, Integer streetId, Integer houseId, @Nullable Integer entranceId,
                                  @Nullable Integer floorId, @Nullable Integer apartId, @Nullable Integer tableIndex) throws Exception {
            Address address = new Address();
            final String streetRawValue = get(streetId, tableIndex).getText();
            final String houseRawValue = get(houseId, tableIndex).getText();
            final String apartRawValue = get(apartId, tableIndex).getText();
            final String entranceRawValue = get(entranceId, tableIndex).getText();
            final String floorRawValue = get(floorId, tableIndex).getText();

            if (streetRawValue == null || streetRawValue.equals("Корпоратив")) return null;
            if (houseRawValue == null) return null;
//            if(apartRawValue == null) return null;
//            if(entranceRawValue == null) return null;
//            if(floorRawValue == null) return null;

            address.setCity(city);
            Street existStreet = context.streetDispatcher.getStreetContainsSubstring(streetRawValue, city.getCityId());
            if (existStreet == null) {
                context.addressCorrectingPool.computeIfAbsent(tempId, (key) -> {
                    AddressCorrecting addressCorrecting = new AddressCorrecting(address);
                    addressCorrecting.setStreetRaw(streetRawValue);
                    addressCorrecting.getTypes().add(AddressCorrectingType.STREET);
                    return addressCorrecting;
                });
            } else {
                address.setStreet(existStreet);
            }

            // Парсим номер дома
            if (!houseRawValue.equals("-")) {
                try {
                    address.setHouseNum(Short.parseShort(houseRawValue));
                } catch (NumberFormatException ignore) {
                    Matcher houseLetterMatcher = Pattern.compile("^(\\d{1,4})\\s?([А-я])$").matcher(houseRawValue);
                    Matcher houseBuildingMatcher = Pattern.compile("^(\\d{1,4})_(\\d)$").matcher(houseRawValue);
                    Matcher houseFractionMatcher = Pattern.compile("^(\\d{1,4})[\\\\/](\\d{1,4})$").matcher(houseRawValue);
                    Matcher houseLetterBuildingMatcher = Pattern.compile("^(\\d{1,4})([А-я])_(\\d)$").matcher(houseRawValue);
                    Matcher houseLetterBuildingMatcherAlter = Pattern.compile("^(\\d{1,4})([А-я]) \\D+(\\d)$").matcher(houseRawValue);
                    Matcher houseLetterBuildingMatcherAlter1 = Pattern.compile("^(\\d{1,4})([А-я])[\\\\/](\\d)$").matcher(houseRawValue);
                    Matcher houseLetterBuildingMatcherAlter2 = Pattern.compile("^(\\d{1,4})([А-я]) (\\d)\\s?[А-я\\.]+$").matcher(houseRawValue);
                    Matcher houseFractionLetterMatcher = Pattern.compile("^(\\d{1,4})[\\\\/](\\d{1,4})([А-я])$").matcher(houseRawValue);
                    Matcher houseFractionBuildingMatcher = Pattern.compile("^(\\d{1,4})[\\\\/](\\d{1,4})_(\\d)$").matcher(houseRawValue);
                    Matcher houseFractionBuildingMatcherAlter = Pattern.compile("^(\\d{1,4})[\\\\/](\\d{1,4}) \\D+(\\d)$").matcher(houseRawValue);
                    Matcher houseFractionBuildingMatcherAlter2 = Pattern.compile("^(\\d{1,4})[\\\\/](\\d{1,4}) (\\d)\\s?с[троение.]*$").matcher(houseRawValue);
                    Matcher houseFractionLetterBuildingMatcher = Pattern.compile("^(\\d{1,4})[\\\\/](\\d{1,4})([А-я])_(\\d)$").matcher(houseRawValue);

                    if (houseLetterMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseLetterMatcher.group(1)));
                        address.setLetter(houseLetterMatcher.group(2).charAt(0));
                    } else if (houseBuildingMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseBuildingMatcher.group(1)));
                        address.setBuild(Short.parseShort(houseBuildingMatcher.group(2)));
                    } else if (houseFractionMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseFractionMatcher.group(1)));
                        address.setFraction(Short.parseShort(houseFractionMatcher.group(2)));
                    } else if (houseLetterBuildingMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseLetterBuildingMatcher.group(1)));
                        address.setLetter(houseLetterBuildingMatcher.group(2).charAt(0));
                        address.setBuild(Short.parseShort(houseLetterBuildingMatcher.group(3)));
                    } else if (houseLetterBuildingMatcherAlter.find()) {
                        address.setHouseNum(Short.parseShort(houseLetterBuildingMatcherAlter.group(1)));
                        address.setLetter(houseLetterBuildingMatcherAlter.group(2).charAt(0));
                        address.setBuild(Short.parseShort(houseLetterBuildingMatcherAlter.group(3)));
                    } else if (houseLetterBuildingMatcherAlter1.find()) {
                        address.setHouseNum(Short.parseShort(houseLetterBuildingMatcherAlter1.group(1)));
                        address.setLetter(houseLetterBuildingMatcherAlter1.group(2).charAt(0));
                        address.setBuild(Short.parseShort(houseLetterBuildingMatcherAlter1.group(3)));
                    } else if (houseLetterBuildingMatcherAlter2.find()) {
                        address.setHouseNum(Short.parseShort(houseLetterBuildingMatcherAlter2.group(1)));
                        address.setLetter(houseLetterBuildingMatcherAlter2.group(2).charAt(0));
                        address.setBuild(Short.parseShort(houseLetterBuildingMatcherAlter2.group(3)));
                    } else if (houseFractionLetterMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseFractionLetterMatcher.group(1)));
                        address.setFraction(Short.parseShort(houseFractionLetterMatcher.group(2)));
                        address.setLetter(houseFractionLetterMatcher.group(3).charAt(0));
                    } else if (houseFractionBuildingMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseFractionBuildingMatcher.group(1)));
                        address.setFraction(Short.parseShort(houseFractionBuildingMatcher.group(2)));
                        address.setBuild(Short.parseShort(houseFractionBuildingMatcher.group(3)));
                    } else if (houseFractionBuildingMatcherAlter.find()) {
                        address.setHouseNum(Short.parseShort(houseFractionBuildingMatcherAlter.group(1)));
                        address.setFraction(Short.parseShort(houseFractionBuildingMatcherAlter.group(2)));
                        address.setBuild(Short.parseShort(houseFractionBuildingMatcherAlter.group(3)));
                    } else if (houseFractionBuildingMatcherAlter2.find()) {
                        address.setHouseNum(Short.parseShort(houseFractionBuildingMatcherAlter2.group(1)));
                        address.setFraction(Short.parseShort(houseFractionBuildingMatcherAlter2.group(2)));
                        address.setBuild(Short.parseShort(houseFractionBuildingMatcherAlter2.group(3)));
                    } else if (houseFractionLetterBuildingMatcher.find()) {
                        address.setHouseNum(Short.parseShort(houseFractionLetterBuildingMatcher.group(1)));
                        address.setFraction(Short.parseShort(houseFractionLetterBuildingMatcher.group(2)));
                        address.setLetter(houseFractionLetterBuildingMatcher.group(3).charAt(0));
                        address.setBuild(Short.parseShort(houseFractionLetterBuildingMatcher.group(4)));
                    } else {
                        context.addressCorrectingPool.computeIfPresent(tempId, (key, value) -> {
                            value.setHouseRaw(houseRawValue);
                            value.getTypes().add(AddressCorrectingType.HOUSE);
                            return value;
                        });
                        context.addressCorrectingPool.computeIfAbsent(tempId, key -> {
                            AddressCorrecting addressCorrecting = new AddressCorrecting(address);
                            addressCorrecting.setHouseRaw(houseRawValue);
                            addressCorrecting.getTypes().add(AddressCorrectingType.HOUSE);
                            return addressCorrecting;
                        });
                    }
                }
            }

            // Парсим номер квартиры
            if (apartRawValue != null && !apartRawValue.equals("-")) {
                try {
                    address.setApartmentNum(Short.parseShort(apartRawValue));
                } catch (NumberFormatException ignore) {
                    Matcher apartLetterMatcher = Pattern.compile("^(\\d{1,3})\\s?([А-я])$").matcher(apartRawValue);
                    Matcher simpleStringMatcher = Pattern.compile("^([А-я]+)$").matcher(apartRawValue);
                    Matcher entranceFloorMatcher = Pattern.compile("^(\\d{1,2})[-ый ]{0,3}\\s?п[одъьезд,\\.]*\\s?(\\d{0,2})[-ый ]{0,3}\\s?э[таж\\.]*$").matcher(apartRawValue);
                    Matcher entranceFloorMatcherAlter = Pattern.compile("^п[одъьезд,\\.]*\\s?(\\d{1,2})[-ый ]{0,3}\\s?э[таж\\.]*\\s?(\\d{0,2})[-ый ]{0,3}$").matcher(apartRawValue);
                    Matcher floorOnlyMatcher = Pattern.compile("^(\\d{0,2})[-ый ]{0,3}\\s?э[таж\\.]*$").matcher(apartRawValue);
                    Matcher floorOnlyMatcherAlter = Pattern.compile("^э[таж\\.]*\\s?(\\d{0,2})[-ый ]{0,3}$").matcher(apartRawValue);
                    Matcher entranceOnlyMatcher = Pattern.compile("^(\\d{1,2})[-ый ]{0,3}\\s?п[одъьезд,\\.]*$").matcher(apartRawValue);
                    Matcher entranceOnlyMatcherAlter = Pattern.compile("^п[одъьезд,\\.]*\\s?(\\d{1,2})[-ый ]{0,3}$").matcher(apartRawValue);
                    Matcher companyNameMatcher = Pattern.compile("^[А-я]+\\s?[А-я]*\\s?[А-я]*$").matcher(apartRawValue);
                    Matcher officeMatcher = Pattern.compile("^оф[ис\\.]{0,3}\\s?(\\d{1,3})$").matcher(apartRawValue);
                    Matcher officeMatcherAlter = Pattern.compile("^(\\d{1,3})\\s?оф[ис\\.]{0,3}$").matcher(apartRawValue);
                    Matcher pavilionMatcher = Pattern.compile("^пав[ильон\\.]{0,6}\\s?(\\d{1,3})$").matcher(apartRawValue);
                    Matcher pavilionMatcherAlter = Pattern.compile("^(\\d{1,3})\\s?пав[ильон\\.]{0,6}$").matcher(apartRawValue);
                    Matcher roomMatcher = Pattern.compile("^пом[ещни\\.]{0,7}\\s?(\\d{1,3})$").matcher(apartRawValue);
                    Matcher roomMatcherAlter = Pattern.compile("^(\\d{1,3})\\s?пом[ещни\\.]{0,7}$").matcher(apartRawValue);

                    if (apartLetterMatcher.find()) {
                        address.setApartmentNum(Short.parseShort(apartLetterMatcher.group(1)));
                        address.setApartmentMod(apartLetterMatcher.group(2));
                    } else if (officeMatcher.find()) {
                        address.setApartmentNum(Short.parseShort(officeMatcher.group(1)));
                        address.setApartmentMod("офис");
                    } else if (officeMatcherAlter.find()) {
                        address.setApartmentNum(Short.parseShort(officeMatcherAlter.group(1)));
                        address.setApartmentMod("офис");
                    } else if (pavilionMatcher.find()) {
                        address.setApartmentNum(Short.parseShort(pavilionMatcher.group(1)));
                        address.setApartmentMod("павильон");
                    } else if (pavilionMatcherAlter.find()) {
                        address.setApartmentNum(Short.parseShort(pavilionMatcherAlter.group(1)));
                        address.setApartmentMod("павильон");
                    } else if (roomMatcher.find()) {
                        address.setApartmentNum(Short.parseShort(roomMatcher.group(1)));
                        address.setApartmentMod("помещение");
                    } else if (roomMatcherAlter.find()) {
                        address.setApartmentNum(Short.parseShort(roomMatcherAlter.group(1)));
                        address.setApartmentMod("помещение");
                    } else if (simpleStringMatcher.find()) {
                        address.setApartmentMod(simpleStringMatcher.group(1));
                    } else if (entranceFloorMatcher.find()) {
                        address.setEntrance(Short.parseShort(entranceFloorMatcher.group(1)));
                        address.setFloor(Short.parseShort(entranceFloorMatcher.group(2)));
                    } else if (floorOnlyMatcher.find()) {
                        address.setFloor(Short.parseShort(floorOnlyMatcher.group(1)));
                    } else if (floorOnlyMatcherAlter.find()) {
                        address.setFloor(Short.parseShort(floorOnlyMatcherAlter.group(1)));
                    } else if (entranceFloorMatcherAlter.find()) {
                        address.setEntrance(Short.parseShort(entranceFloorMatcherAlter.group(1)));
                        address.setFloor(Short.parseShort(entranceFloorMatcherAlter.group(2)));
                    } else if (entranceOnlyMatcher.find()) {
                        address.setEntrance(Short.parseShort(entranceOnlyMatcher.group(1)));
                    } else if (entranceOnlyMatcherAlter.find()) {
                        address.setEntrance(Short.parseShort(entranceOnlyMatcherAlter.group(1)));
                    } else if (companyNameMatcher.find()) {
                        address.setApartmentMod(companyNameMatcher.group());
                    } else {
                        context.addressCorrectingPool.computeIfPresent(tempId, (key, value) -> {
                            value.setApartRaw(apartRawValue);
                            value.getTypes().add(AddressCorrectingType.APART);
                            return value;
                        });
                        context.addressCorrectingPool.computeIfAbsent(tempId, key -> {
                            AddressCorrecting addressCorrecting = new AddressCorrecting(address);
                            addressCorrecting.setApartRaw(apartRawValue);
                            addressCorrecting.getTypes().add(AddressCorrectingType.APART);
                            return addressCorrecting;
                        });
                    }
                }
            }

            if (entranceRawValue != null) {
                try {
                    address.setEntrance(Short.parseShort(entranceRawValue));
                } catch (NumberFormatException ignore) {
                }
            }

            if (floorRawValue != null) {
                try {
                    address.setFloor(Short.parseShort(floorRawValue));
                } catch (NumberFormatException ignore) {
                }
            }

            if (context.addressCorrectingPool.containsKey(tempId)) {
                throw new Exception("Адрес добавлен в список для корректировки");
            }

            return address;
        }

        public Address getAddress(UUID tempId, City city, Integer streetId, Integer houseId, @Nullable Integer entranceId,
                                  @Nullable Integer floorId, @Nullable Integer apartId) throws Exception {
            return getAddress(tempId, city, streetId, houseId, entranceId, floorId, apartId, null);
        }
    }
}
