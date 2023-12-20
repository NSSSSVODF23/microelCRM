package com.microel.trackerbackend.services.external.oldtracker.task;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import com.microel.trackerbackend.storage.entities.task.WorkReport;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class TaskClassOT {
    @NonNull
    private Integer id;
    @NonNull
    private String name;
    @NonNull
    private List<TaskStageOT> stages;
    @NonNull
    private List<TaskFieldOT> fields;
    @NonNull
    private GenericFieldsCreation standardFieldsOnCreation;
    @NonNull
    private GenericFieldsAssignation standardFieldsOnAssignation;
    @NonNull
    private GenericFieldsReport standardFieldsOnReport;

    public TaskFieldOT getFieldById(Integer fieldId){
        return fields.stream().filter(field -> field.getId().equals(fieldId)).findFirst().orElseThrow(()->new ResponseException("Поле "+fieldId+" не найдено в классе "+getId()));
    }

    public boolean isStageExist(Integer taskStageId) {
        return stages.stream().anyMatch(stage -> stage.getId().equals(taskStageId));
    }

    @FunctionalInterface
    public interface GenericFieldsCreation{
        List<OldTrackerRequestFactory.FieldData> get();
    }

    @FunctionalInterface
    public interface GenericFieldsAssignation{
        List<OldTrackerRequestFactory.FieldData> get(Employee... employees);
    }

    @FunctionalInterface
    public interface GenericFieldsReport{
        List<OldTrackerRequestFactory.FieldData> get(WorkReport... workReports);
    }
}
