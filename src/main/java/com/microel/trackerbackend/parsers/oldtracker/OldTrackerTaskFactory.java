package com.microel.trackerbackend.parsers.oldtracker;

import com.microel.trackerbackend.parsers.oldtracker.bindings.*;
import com.microel.trackerbackend.storage.dto.mapper.CommentMapper;
import com.microel.trackerbackend.storage.dto.mapper.EmployeeMapper;
import com.microel.trackerbackend.storage.dto.mapper.ModelItemMapper;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.dto.templating.TaskStageDto;
import com.microel.trackerbackend.storage.dto.templating.WireframeDto;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OldTrackerTaskFactory {
    @NonNull
    private final BindingsCollection bindings;
    @NonNull
    private final Employee author;
    @NonNull
    private final LocalDateTime creationTimestamp;
    @NonNull
    private final List<Comment> commentsList;
    @NonNull
    private final List<WireframeDto> wireframeDtoList;
    @NonNull
    private final String stageName;

    public TaskDto createAccident(String login, @Nullable Address address, String phone, String description, String workReport) {
        List<ModelItem> fields = new ArrayList<>();
        AccidentBindings accidentBindings = bindings.getAccident();

        fields.add(ModelItem.builder().id(accidentBindings.getLogin()).name("Логин")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(login).build());

        fields.add(ModelItem.builder().id(accidentBindings.getAddress()).name("Адрес")
                .wireframeFieldType(WireframeFieldType.ADDRESS)
                .addressData(address).build());

        fields.add(ModelItem.builder().id(accidentBindings.getDescription()).name("Описание проблемы")
                .wireframeFieldType(WireframeFieldType.LARGE_TEXT).stringData(description).build());

        fields.add(ModelItem.builder().id(accidentBindings.getWorkReport()).name("Отчет о работе")
                .wireframeFieldType(WireframeFieldType.LARGE_TEXT).stringData(workReport).build());

        fields.add(ModelItem.builder().id(accidentBindings.getPhone()).name("Телефон")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(phone).build());

        WireframeDto wireframe = wireframeDtoList.stream().filter(w -> w.getWireframeId().equals(bindings.getAccident().getWireframe().getWireframeId())).findFirst().orElse(null);

        return getTask(wireframe, fields);
    }

    public TaskDto createConnection(String takenFrom, String type, String login, String password, String fullName,
                                    Address address, String phone, String advertisingSource, String techInfo) {
        List<ModelItem> fields = new ArrayList<>();
        ConnectionBindings connectionBindings = bindings.getConnection();

        fields.add(ModelItem.builder().id(connectionBindings.getTakenFrom()).name("Источник")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(takenFrom).build());

        fields.add(ModelItem.builder().id(connectionBindings.getType()).name("Тип")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(type).build());

        fields.add(ModelItem.builder().id(connectionBindings.getLogin()).name("Логин")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(login).build());

        fields.add(ModelItem.builder().id(connectionBindings.getPassword()).name("Пароль")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(password).build());

        fields.add(ModelItem.builder().id(connectionBindings.getFullName()).name("ФИО")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(fullName).build());

        fields.add(ModelItem.builder().id(connectionBindings.getAddress()).name("Адрес")
                .wireframeFieldType(WireframeFieldType.ADDRESS)
                .addressData(address).build());

        fields.add(ModelItem.builder().id(connectionBindings.getPhone()).name("Телефон")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(phone).build());

        fields.add(ModelItem.builder().id(connectionBindings.getAdvertisingSource()).name("Рекламный источник")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(advertisingSource).build());

        fields.add(ModelItem.builder().id(connectionBindings.getTechInfo()).name("Тех. информация")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(techInfo).build());

        WireframeDto wireframe = wireframeDtoList.stream().filter(w -> w.getWireframeId().equals(bindings.getConnection().getWireframe().getWireframeId())).findFirst().orElse(null);
        return getTask(wireframe, fields);
    }

    public TaskDto createPrivateSectorVD(String district, @Nullable Address address, String phone, String name, String takenFrom) {
        List<ModelItem> fields = new ArrayList<>();
        PrivateSectorVD privateSectorVDBindings = bindings.getPrivateSectorVD();

        fields.add(ModelItem.builder().id(privateSectorVDBindings.getDistrict()).name("Район")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(district).build());

        fields.add(ModelItem.builder().id(privateSectorVDBindings.getPhone()).name("Телефон")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(phone).build());

        fields.add(ModelItem.builder().id(privateSectorVDBindings.getName()).name("Имя")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(name).build());

        fields.add(ModelItem.builder().id(privateSectorVDBindings.getAdvertisingSource()).name("Рекламный источник")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(takenFrom).build());

        fields.add(ModelItem.builder().id(privateSectorVDBindings.getAddress()).name("Адрес")
                .wireframeFieldType(WireframeFieldType.ADDRESS)
                .addressData(address).build());

        WireframeDto wireframe = wireframeDtoList.stream().filter(w -> w.getWireframeId().equals(bindings.getPrivateSectorVD().getWireframe().getWireframeId())).findFirst().orElse(null);

        return getTask(wireframe, fields);
    }

    public TaskDto createPrivateSectorRM(String gardening, @Nullable Address address, String name, String phone, String advertisingSource) {
        List<ModelItem> fields = new ArrayList<>();
        PrivateSectorRM privateSectorRMBindings = bindings.getPrivateSectorRM();

        fields.add(ModelItem.builder().id(privateSectorRMBindings.getGardening()).name("Садоводство")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(gardening).build());

        fields.add(ModelItem.builder().id(privateSectorRMBindings.getPhone()).name("Телефон")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(phone).build());

        fields.add(ModelItem.builder().id(privateSectorRMBindings.getName()).name("Имя")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(name).build());

        fields.add(ModelItem.builder().id(privateSectorRMBindings.getAdvertisingSource()).name("Рекламный источник")
                .wireframeFieldType(WireframeFieldType.SMALL_TEXT).stringData(advertisingSource).build());

        fields.add(ModelItem.builder().id(privateSectorRMBindings.getAddress()).name("Адрес")
                .wireframeFieldType(WireframeFieldType.ADDRESS)
                .addressData(address).build());

        WireframeDto wireframe = wireframeDtoList.stream().filter(w -> w.getWireframeId().equals(bindings.getPrivateSectorRM().getWireframe().getWireframeId())).findFirst().orElse(null);

        return getTask(wireframe, fields);
    }

    private TaskDto getTask(WireframeDto wireframe, List<ModelItem> fields) {
        TaskDto task = new TaskDto();

        TaskStageDto taskStage = null;
        if (wireframe != null) {
            taskStage = wireframe.getStageByName(stageName);
        }

        task.setCurrentStage(taskStage);
        task.setCreated(Timestamp.valueOf(creationTimestamp));
        task.setFields(fields.stream().map(ModelItemMapper::toDto).collect(Collectors.toList()));
        task.setModelWireframe(wireframe);
        task.setComments(commentsList.stream().map(CommentMapper::toDto).collect(Collectors.toList()));
        task.setTaskStatus(TaskStatus.CLOSE);
        task.setCreator(EmployeeMapper.toDto(author));

        return task;
    }
}
