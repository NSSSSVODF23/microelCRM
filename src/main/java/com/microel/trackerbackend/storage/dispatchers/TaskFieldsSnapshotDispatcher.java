package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskFieldsSnapshot;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.TaskFieldsSnapshotRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class TaskFieldsSnapshotDispatcher {
    private final TaskFieldsSnapshotRepository taskFieldsSnapshotRepository;
    private final TaskDispatcher taskDispatcher;
    private final EmployeeDispatcher employeeDispatcher;


    public TaskFieldsSnapshotDispatcher(TaskFieldsSnapshotRepository taskFieldsSnapshotRepository, TaskDispatcher taskDispatcher, EmployeeDispatcher employeeDispatcher) {
        this.taskFieldsSnapshotRepository = taskFieldsSnapshotRepository;
        this.taskDispatcher = taskDispatcher;
        this.employeeDispatcher = employeeDispatcher;
    }

    public SnapshotBuilder builder() {
        return new SnapshotBuilder(taskFieldsSnapshotRepository, taskDispatcher, employeeDispatcher);
    }

    public List<TaskFieldsSnapshot> getTaskFieldsSnapshots(Long targetTask) throws EntryNotFound {
        Task task = taskDispatcher.getTask(targetTask);
        return taskFieldsSnapshotRepository.findByTask(task, Sort.by(Sort.Direction.DESC, "whenEdited"));
    }

    static public class SnapshotBuilder {
        private final TaskFieldsSnapshot taskFieldsSnapshot = new TaskFieldsSnapshot();
        private final TaskFieldsSnapshotRepository taskFieldsSnapshotRepository;
        private final TaskDispatcher taskDispatcher;
        private final EmployeeDispatcher employeeDispatcher;
        private String beforeChange = null;
        private String afterChange = null;

        public SnapshotBuilder(TaskFieldsSnapshotRepository taskFieldsSnapshotRepository, TaskDispatcher taskDispatcher, EmployeeDispatcher employeeDispatcher) {
            this.taskFieldsSnapshotRepository = taskFieldsSnapshotRepository;
            this.taskDispatcher = taskDispatcher;
            this.employeeDispatcher = employeeDispatcher;
        }

        public SnapshotBuilder beforeEditing(Long targetTask, Employee whoEdited) throws EntryNotFound, JsonProcessingException {
            Task task = this.taskDispatcher.getTask(targetTask);
            Employee employee = this.employeeDispatcher.getEmployee(whoEdited.getLogin());
            taskFieldsSnapshot.setTask(task);
            taskFieldsSnapshot.setWhoEdited(employee);
            beforeChange = new ObjectMapper().writeValueAsString(task.getFields());
            return this;
        }

        public SnapshotBuilder afterEditing() throws EntryNotFound, JsonProcessingException {
            Task task = this.taskDispatcher.getTask(taskFieldsSnapshot.getTask().getTaskId());
            afterChange = new ObjectMapper().writeValueAsString(task.getFields());
            return this;
        }

        public void flush() throws EntryNotFound, JsonProcessingException {
            ObjectMapper om = new ObjectMapper();
            this.taskFieldsSnapshot.setBeforeEditing(om.readValue(beforeChange, new TypeReference<List<ModelItem>>(){}));
            this.taskFieldsSnapshot.setAfterEditing(om.readValue(afterChange, new TypeReference<List<ModelItem>>(){}));
            this.taskFieldsSnapshot.setWhenEdited(Timestamp.from(Instant.now()));
            this.taskFieldsSnapshotRepository.save(taskFieldsSnapshot);
        }

    }
}
