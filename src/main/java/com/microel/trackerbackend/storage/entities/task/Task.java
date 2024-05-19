package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.TimeFrame;
import com.microel.trackerbackend.misc.task.counting.*;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskClassOT;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import com.microel.trackerbackend.services.external.oldtracker.task.fields.StreetFieldOT;
import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher.FiltrationConditions.SchedulingType;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.utils.TaskGroup;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.templating.*;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.fields.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.javatuples.Pair;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "tasks")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    private Timestamp created;
    private Timestamp updated;
    private Timestamp closed;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_last_comment_id")
    @OrderBy("created asc")
    @BatchSize(size = 15)
//    @Fetch(FetchMode.SUBSELECT)
    private List<Comment> lastComments;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_creator_id")
    private Employee creator;

    private Timestamp actualFrom;

    private Timestamp actualTo;
    private TaskStatus taskStatus;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleted = false;

    @ManyToMany()
    @OrderBy(value = "name")
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<TaskTag> tags;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_model_wireframe_id")
    private Wireframe modelWireframe;

    @OneToMany(mappedBy = "task", targetEntity = ModelItem.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JsonManagedReference
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<ModelItem> fields;
    @JsonIgnore
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<Comment> comments;
    @JsonIgnore
    @OneToMany(mappedBy = "task", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<TaskEvent> taskEvents;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_responsible_employee")
    private Employee responsible;
    @ManyToMany()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<Employee> employeesObservers;
    @ManyToMany()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<Department> departmentsObservers;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private TaskStage currentStage;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private TaskTypeDirectory currentDirectory;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_group_id")
    private TaskGroup group;

    private Long parent;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_parent_id")
    @BatchSize(size = 25)
//    @Fetch(FetchMode.SUBSELECT)
    private List<Task> children;

    @Nullable
    private Long oldTrackerTaskId;
    @Nullable
    private Integer oldTrackerTaskClassId;
    @Nullable
    private Integer oldTrackerCurrentStageId;

    @Nullable
    @OneToMany(mappedBy = "task")
//    @Fetch(FetchMode.SUBSELECT)
    @BatchSize(size = 25)
    @JsonIgnore
    private List<WorkLog> workLogs;

    @JsonIgnore
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getModelWireframe().getName())
                .append(" - ")
                .append(getCurrentStage().getLabel());
        if (getCurrentDirectory() != null) {
            sb.append(" (")
                    .append(getCurrentDirectory().getName())
                    .append(")");
        }
        ModelItem addressItem = getFields().stream()
                .filter(item -> item.getWireframeFieldType() == WireframeFieldType.ADDRESS)
                .findFirst().orElse(null);
        if (addressItem != null &&
                addressItem.getAddressData() != null &&
                addressItem.getAddressData().getAddressName() != null &&
                !addressItem.getAddressData().getAddressName().isBlank()) {
            sb.append(" • ")
                    .append(addressItem.getAddressData().getAddressName());
        } else {
            ModelItem shortTextItem = getFields().stream()
                    .filter(item -> item.getWireframeFieldType() == WireframeFieldType.SMALL_TEXT)
                    .findFirst().orElse(null);
            if (shortTextItem != null &&
                    shortTextItem.getStringData() != null &&
                    !shortTextItem.getStringData().isBlank()) {
                sb.append(" • ")
                        .append(shortTextItem.getStringData());
            } else if (!getLastComments().isEmpty()) {
                Comment comment = getLastComments().get(0);
                sb.append(" • ")
                        .append(comment.getSimpleText());
            }
        }
        return sb.toString();
    }

    @JsonIgnore
    public String getClassName() {
        if (getModelWireframe() != null) {
            return getModelWireframe().getName();
        }
        return "Неизвестно";
    }

    @JsonIgnore
    public String getTypeName() {
        if (getCurrentStage() != null) {
            return getCurrentStage().getLabel();
        }
        return "Неизвестно";
    }

    @JsonIgnore
    public List<AbstractTaskCounterPath> getListOfCounterPaths() {
        TimeFrame[] closeTaskTimeFrames = new TimeFrame[]{TimeFrame.TODAY, TimeFrame.YESTERDAY, TimeFrame.LAST_WEEK,
                TimeFrame.THIS_WEEK, TimeFrame.LAST_MONTH, TimeFrame.THIS_MONTH};
        TimeFrame[] scheduledTaskTimeFrames = new TimeFrame[]{TimeFrame.TODAY, TimeFrame.TOMORROW, TimeFrame.THIS_WEEK,
                TimeFrame.NEXT_WEEK, TimeFrame.THIS_MONTH, TimeFrame.NEXT_MONTH};
        List<AbstractTaskCounterPath> collect = new ArrayList<>();
        if (getTaskStatus() != null) {
            switch (getTaskStatus()) {
                case ACTIVE -> {
                    if (getActualFrom() == null) {
                        collect.add(TaskStatusPath.of(SchedulingType.EXCEPT_PLANNED, getTaskStatus()));
                        if (getModelWireframe() != null) {
                            collect.add(TaskClassPath.of(SchedulingType.EXCEPT_PLANNED, getTaskStatus(), getModelWireframe().getWireframeId()));
                            if (getCurrentStage() != null) {
                                collect.add(TaskTypePath.of(SchedulingType.EXCEPT_PLANNED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId()));
                                if (getCurrentDirectory() != null) {
                                    collect.add(TaskDirectoryPath.of(SchedulingType.EXCEPT_PLANNED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId(), getCurrentDirectory().getTaskTypeDirectoryId()));
                                }
                            }
                        }
                    } else {
                        collect.add(TaskStatusPath.of(SchedulingType.PLANNED, getTaskStatus()));
                        if (getModelWireframe() != null) {
                            collect.add(TaskClassPath.of(SchedulingType.PLANNED, getTaskStatus(), getModelWireframe().getWireframeId()));
                            if (getCurrentStage() != null) {
                                collect.add(TaskTypePath.of(SchedulingType.PLANNED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId()));
                                for (TimeFrame timeFrame : scheduledTaskTimeFrames) {
                                    collect.add(TaskScheduleDatePath.of(SchedulingType.PLANNED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId(), timeFrame));
                                }
                            }
                        }
                    }

                    if (getActualTo() != null) {
                        collect.add(TaskStatusPath.of(SchedulingType.DEADLINE, getTaskStatus()));
                        if (getModelWireframe() != null) {
                            collect.add(TaskClassPath.of(SchedulingType.DEADLINE, getTaskStatus(), getModelWireframe().getWireframeId()));
                            if (getCurrentStage() != null) {
                                collect.add(TaskTypePath.of(SchedulingType.DEADLINE, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId()));
                                for (TimeFrame timeFrame : scheduledTaskTimeFrames) {
                                    collect.add(TaskTermDatePath.of(SchedulingType.DEADLINE, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId(), timeFrame));
                                }
                            }
                        }
                    }
                }
                case PROCESSING -> {
                    collect.add(TaskClassPath.of(SchedulingType.UNSCHEDULED, getTaskStatus()));
                    if (getModelWireframe() != null) {
                        collect.add(TaskClassPath.of(SchedulingType.UNSCHEDULED, getTaskStatus(), getModelWireframe().getWireframeId()));
                        if (getCurrentStage() != null) {
                            collect.add(TaskTypePath.of(SchedulingType.UNSCHEDULED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId()));
                        }
                    }
                }
                case CLOSE -> {
                    collect.add(TaskClassPath.of(SchedulingType.UNSCHEDULED, getTaskStatus()));
                    if (getModelWireframe() != null) {
                        collect.add(TaskClassPath.of(SchedulingType.UNSCHEDULED, getTaskStatus(), getModelWireframe().getWireframeId()));
                        if (getCurrentStage() != null) {
                            collect.add(TaskTypePath.of(SchedulingType.UNSCHEDULED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId()));
                            for (TimeFrame timeFrame : closeTaskTimeFrames) {
                                collect.add(TaskClosingDatePath.of(SchedulingType.UNSCHEDULED, getTaskStatus(), getModelWireframe().getWireframeId(), getCurrentStage().getStageId(), timeFrame));
                            }
                        }
                    }
                }
            }
        }
        return collect;
    }

    /**
     * Создает отсортированный список полей для отображения в элементе списка
     *
     * @return Список полей задачи
     */
    public List<ModelItem> getListItemFields() {
        List<ModelItem> collect = modelWireframe.getAllFields().stream()
                .filter(f -> f.getListViewIndex() != null)
                .sorted(Comparator.nullsLast(Comparator.comparing(FieldItem::getListViewIndex)))
                .map(fieldItem -> fields.stream().filter(f -> f.getId().equals(fieldItem.getId())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        collect.addAll(fields.stream().filter(modelItem -> {
            return modelWireframe.getAllFields().stream().noneMatch(mfi -> Objects.equals(mfi.getId(), modelItem.getId()));
        }).toList());

        return collect;
    }

    public List<ModelItem> getFields() {
        List<ModelItem> collect = modelWireframe.getAllFields().stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(FieldItem::getOrderPosition))).filter(Objects::nonNull)
                .map(fieldItem -> fields.stream().filter(f -> Objects.equals(f.getId(), fieldItem.getId())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        collect.addAll(fields.stream().filter(modelItem -> {
            return modelWireframe.getAllFields().stream().noneMatch(mfi -> Objects.equals(mfi.getId(), modelItem.getId()));
        }).toList());
        return collect;
    }

    public void setFields(List<ModelItem> fieldsAppend) {
        fields = fieldsAppend.stream().peek(f -> f.setTask(this)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String sb = "Задача {" + "taskId=" + taskId +
                ", Создана=" + created +
                ", Статус=" + taskStatus +
                ", Шаблон=" + modelWireframe +
                ", Поля=" + fields +
                '}';
        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return Objects.equals(getTaskId(), task.getTaskId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskId());
    }

    public void editFields(List<ModelItem> fields) {
        if (fields == null) return;
        // Заменяем поле если оно уже существует
        this.fields.forEach(f -> {
            ModelItem newField = fields.stream().filter(ff -> ff.getModelItemId().equals(f.getModelItemId())).findFirst().orElse(null);
            if (newField == null) return;
            Object oldValue = f.getValue();
            if (oldValue != null && newField.getValue() != null && !oldValue.equals(newField.getValue())) {
                f.setValue(newField.getValue());
            } else if (oldValue == null && newField.getValue() != null) {
                f.setValue(newField.getValue());
            }
        });
    }

    public void setComments(List<Comment> commentsAppend) {
        comments = commentsAppend.stream().peek(f -> f.setParent(this)).collect(Collectors.toList());
    }

    public void appendComment(Comment comment) {
        comment.setParent(this);
        comments.add(comment);
    }

    public void setChildren(List<Task> childrenAppend) {
        children = childrenAppend.stream().peek(f -> f.setParent(taskId)).collect(Collectors.toList());
    }

    public void setTags(Set<TaskTag> tagsAppend) {
        tags = tagsAppend.stream().peek(f -> {
            f.setTask(new HashSet<>());
            f.getTask().add(this);
        }).collect(Collectors.toList());
    }

    public Set<Employee> getAllEmployeesObservers(Employee exclude) {
        Set<Employee> allEmployeeObservers = new HashSet<>();
        if (employeesObservers != null) {
            allEmployeeObservers.addAll(employeesObservers);
        }
        if (departmentsObservers != null) {
            for (Department department : departmentsObservers) {
                Set<Employee> departmentEmployees = department.getEmployees();
                if (departmentEmployees != null) allEmployeeObservers.addAll(departmentEmployees);
            }
        }
        if (exclude != null) allEmployeeObservers.remove(exclude);
        return allEmployeeObservers;
    }

    public Set<Employee> getAllEmployeesObservers() {
        return getAllEmployeesObservers(null);
    }

    public void appendEvent(TaskEvent taskEvent) {
        if (getTaskEvents() == null)
            setTaskEvents(new ArrayList<>());
        getTaskEvents().add(taskEvent);
    }

    @JsonIgnore
    @Transient
    public List<OldTrackerRequestFactory.FieldData> getFieldsForOldTracker(@NonNull TaskClassOT taskClassOT) {
        if (currentStage.getOldTrackerBind() == null)
            throw new ResponseException("Типу задачи не установлена конфигурация привязки");
        List<FieldDataBind> fieldDataBinds = currentStage.getOldTrackerBind().getFieldDataBinds();
        List<OldTrackerRequestFactory.FieldData> result = new ArrayList<>();
        Pair<Integer, String> addressBackup = null;
        for (FieldDataBind dataBind : fieldDataBinds) {
            ModelItem modelItem = fields.stream().filter(f -> Objects.equals(dataBind.getFieldItemId(), f.getId())).findFirst().orElse(null);
            if (modelItem == null) continue;
            switch (modelItem.getWireframeFieldType()) {
                case ADDRESS:
                    if (!(dataBind instanceof AddressFieldDataBind addressDataBind) || modelItem.getAddressData() == null || modelItem.getAddressData().getStreet() == null || modelItem.getAddressData().getHouseNum() == null)
                        break;
                    StreetFieldOT streetDropdownField = (StreetFieldOT) taskClassOT.getFieldById(addressDataBind.getStreetFieldId());
                    Long internalStreetId = modelItem.getAddressData().getStreet().getStreetId();
                    Pair<Integer, String> outerStreetPair = streetDropdownField.getStreetsBindings().get(internalStreetId);
                    Integer outerStreetId = null;
                    if (outerStreetPair == null) {
                        if (addressDataBind.getBackupFieldId() != null) {
                            addressBackup = new Pair<>(addressDataBind.getBackupFieldId(), modelItem.getAddressData().getHouseName());
                        }
                    } else {
                        outerStreetId = outerStreetPair.getValue0();
                    }
                    if (addressDataBind.getStreetFieldId() != null)
                        result.add(new OldTrackerRequestFactory.FieldData(addressDataBind.getStreetFieldId(), TaskFieldOT.Type.STREET, Utils.stringConvertor(outerStreetId).orElse(streetDropdownField.getDefaultNullStreet().toString())));
                    if (addressDataBind.getHouseFieldId() != null)
                        result.add(new OldTrackerRequestFactory.FieldData(addressDataBind.getHouseFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(modelItem.getAddressData().getHouseNamePart()).orElse("-")));
                    if (addressDataBind.getApartmentFieldId() != null)
                        result.add(new OldTrackerRequestFactory.FieldData(addressDataBind.getApartmentFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(modelItem.getAddressData().getApartmentNum()).orElse("-")));
                    if (addressDataBind.getEntranceFieldId() != null)
                        result.add(new OldTrackerRequestFactory.FieldData(addressDataBind.getEntranceFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(modelItem.getAddressData().getEntrance()).orElse("-")));
                    if (addressDataBind.getFloorFieldId() != null)
                        result.add(new OldTrackerRequestFactory.FieldData(addressDataBind.getFloorFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(modelItem.getAddressData().getFloor()).orElse("-")));
                    break;
                case AD_SOURCE:
                    if (!(dataBind instanceof AdSourceFieldDataBind adSourceDataBind))
                        break;
                    if (adSourceDataBind.getAdSourceFieldId() != null) {
                        try {
                            AdvertisingSource advertisingSource = modelItem.getStringData() == null ? null : AdvertisingSource.valueOf(modelItem.getStringData());
                            result.add(new OldTrackerRequestFactory.FieldData(adSourceDataBind.getAdSourceFieldId(), TaskFieldOT.Type.AD_SOURCE, Utils.stringConvertor(advertisingSource == null ? null : advertisingSource.ordinal()).orElse("1")));
                        } catch (IllegalArgumentException e) {
                            break;
                        }
                    }
                    break;
                case CONNECTION_TYPE:
                    if (!(dataBind instanceof ConnectionTypeFieldDataBind connectionTypeFieldDataBind) || modelItem.getStringData() == null)
                        break;
                    ModelItem conServField = fields.stream().filter(mi -> Objects.equals(mi.getId(), connectionTypeFieldDataBind.getConnectionServicesInnerFieldId())).findFirst().orElse(null);
                    if (conServField == null) break;
                    try {
                        ConnectionType connectionType = ConnectionType.valueOf(modelItem.getStringData());
                        List<DataConnectionService> connectionServicesData = conServField.getConnectionServicesData();
                        if (connectionServicesData == null || connectionServicesData.isEmpty()) {
                            result.add(new OldTrackerRequestFactory.FieldData(connectionTypeFieldDataBind.getCtFieldDataBind(), TaskFieldOT.Type.CONNECTION_TYPE, "2"));
                            break;
                        }
                        List<ConnectionService> connectionServices = connectionServicesData.stream().map(DataConnectionService::getConnectionService).toList();
                        switch (connectionType) {
                            case NEW -> {
                                if (connectionServices.contains(ConnectionService.INTERNET) && connectionServices.contains(ConnectionService.CTV)) {
                                    result.add(new OldTrackerRequestFactory.FieldData(connectionTypeFieldDataBind.getCtFieldDataBind(), TaskFieldOT.Type.CONNECTION_TYPE, "1"));
                                } else if (connectionServices.contains(ConnectionService.INTERNET)) {
                                    result.add(new OldTrackerRequestFactory.FieldData(connectionTypeFieldDataBind.getCtFieldDataBind(), TaskFieldOT.Type.CONNECTION_TYPE, "2"));
                                } else if (connectionServices.contains(ConnectionService.CTV)) {
                                    result.add(new OldTrackerRequestFactory.FieldData(connectionTypeFieldDataBind.getCtFieldDataBind(), TaskFieldOT.Type.CONNECTION_TYPE, "3"));
                                }
                            }
                            case RESUMPTION -> {
                                result.add(new OldTrackerRequestFactory.FieldData(connectionTypeFieldDataBind.getCtFieldDataBind(), TaskFieldOT.Type.CONNECTION_TYPE, "5"));
                            }
                            case TRANSFER -> {
                                result.add(new OldTrackerRequestFactory.FieldData(connectionTypeFieldDataBind.getCtFieldDataBind(), TaskFieldOT.Type.CONNECTION_TYPE, "4"));
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                case PASSPORT_DETAILS:
                    if (!(dataBind instanceof PassportDetailsFieldDataBind passportDetailsFieldDataBind) || modelItem.getPassportDetailsData() == null)
                        break;
                    if (passportDetailsFieldDataBind.getPassportSeriesFieldId() != null)
                        result.add(
                                new OldTrackerRequestFactory.FieldData(passportDetailsFieldDataBind.getPassportSeriesFieldId(),
                                        TaskFieldOT.Type.TEXT,
                                        Utils.stringConvertor(modelItem.getPassportDetailsData().getPassportSeries()).orElse("")
                                )
                        );
                    if (passportDetailsFieldDataBind.getPassportNumberFieldId() != null)
                        result.add(
                                new OldTrackerRequestFactory.FieldData(passportDetailsFieldDataBind.getPassportNumberFieldId(),
                                        TaskFieldOT.Type.TEXT,
                                        Utils.stringConvertor(modelItem.getPassportDetailsData().getPassportNumber()).orElse(""))
                        );
                    if (passportDetailsFieldDataBind.getPassportIssuedByFieldId() != null)
                        result.add(
                                new OldTrackerRequestFactory.FieldData(passportDetailsFieldDataBind.getPassportIssuedByFieldId(),
                                        TaskFieldOT.Type.TEXT,
                                        Utils.stringConvertor(modelItem.getPassportDetailsData().getPassportIssuedBy()).orElse(""))
                        );
                    if (passportDetailsFieldDataBind.getPassportIssuedDateFieldId() != null)
                        result.add(
                                new OldTrackerRequestFactory.FieldData(passportDetailsFieldDataBind.getPassportIssuedDateFieldId(),
                                        TaskFieldOT.Type.TEXT,
                                        Utils.stringConvertor(modelItem.getPassportDetailsData().getPassportIssuedDate()).orElse(""))
                        );
                    if (passportDetailsFieldDataBind.getRegistrationAddressFieldId() != null)
                        result.add(
                                new OldTrackerRequestFactory.FieldData(passportDetailsFieldDataBind.getRegistrationAddressFieldId(),
                                        TaskFieldOT.Type.TEXT,
                                        Utils.stringConvertor(modelItem.getPassportDetailsData().getRegistrationAddress()).orElse(""))
                        );
                    break;
                default:
                    if (dataBind instanceof TextFieldDataBind textFieldDataBind) {
                        result.add(new OldTrackerRequestFactory.FieldData(textFieldDataBind.getTextFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(modelItem.getTextRepresentation()).orElse("-")));
                    } else if (dataBind instanceof FullNameFieldDataBind fullNameFieldDataBind) {
                        if (modelItem.getStringData() == null) break;
                        List<String> split = List.of(modelItem.getStringData().split(" "));

                        String lastName = null;
                        try {
                            lastName = split.get(0);
                        } catch (IndexOutOfBoundsException ignore) {
                        }

                        String firstName = null;
                        try {
                            firstName = split.get(1);
                        } catch (IndexOutOfBoundsException ignore) {
                        }

                        String patronymic = null;
                        try {
                            patronymic = split.get(2);
                        } catch (IndexOutOfBoundsException ignore) {
                        }

                        if (fullNameFieldDataBind.getLastNameFieldId() != null) {
                            result.add(new OldTrackerRequestFactory.FieldData(fullNameFieldDataBind.getLastNameFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(lastName).orElse("-")));
                        }
                        if (fullNameFieldDataBind.getFirstNameFieldId() != null) {
                            result.add(new OldTrackerRequestFactory.FieldData(fullNameFieldDataBind.getFirstNameFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(firstName).orElse("-")));
                        }
                        if (fullNameFieldDataBind.getPatronymicFieldId() != null) {
                            result.add(new OldTrackerRequestFactory.FieldData(fullNameFieldDataBind.getPatronymicFieldId(), TaskFieldOT.Type.TEXT, Utils.stringConvertor(patronymic).orElse("-")));
                        }
                    }
                    break;
            }
        }
        if (addressBackup != null) {
            Integer fieldId = addressBackup.getValue0();
            String houseName = addressBackup.getValue1();
            result.forEach(fieldData -> {
                if (Objects.equals(fieldData.getId(), fieldId))
                    fieldData.setData(fieldData.getData() + " - " + houseName);
            });
        }
        return result;
    }

    public Optional<ModelItem> getFieldByIdAndType(String fieldId, WireframeFieldType type) {
        return fields.stream().filter(mi -> Objects.equals(mi.getId(), fieldId) && Objects.equals(mi.getWireframeFieldType(), type)).findFirst();
    }

    /**
     * Объект для получения информации о задаче для создания
     */
    @Getter
    @Setter
    public static class CreationBody {
        private Long wireframeId;
        private List<ModelItem> fields;
        @Nullable
        private Set<TaskTag> tags;
        @Nullable
        private List<DefaultObserver> observers;
        @Nullable
        private Long childId;
        @Nullable
        private Long parentId;
        @Nullable
        private String initialComment;
        @Nullable
        private String type;
        @Nullable
        private Long directory;
        @Nullable
        private Boolean isDuplicateInOldTracker;
    }
}
