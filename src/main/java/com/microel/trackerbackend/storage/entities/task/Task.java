package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.*;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.utils.TaskGroup;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
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
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_last_comment_id")
    private Comment lastComment;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_creator_id")
    private Employee creator;

    private Timestamp actualFrom;

    private Timestamp actualTo;

    private TaskStatus taskStatus;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleted = false;

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private Set<TaskTag> tags;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_model_wireframe_id")
    private Wireframe modelWireframe;

    @OneToMany(mappedBy = "task", targetEntity = ModelItem.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JsonManagedReference
    @BatchSize(size = 25)
    private List<ModelItem> fields;
    @JsonIgnore
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @BatchSize(size = 25)
    private List<Comment> comments;
    @JsonIgnore
    @OneToMany(mappedBy = "task", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @BatchSize(size = 25)
    private List<TaskEvent> taskEvents;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_responsible_employee")
    private Employee responsible;
    @ManyToMany()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @BatchSize(size = 25)
    private List<Employee> employeesObservers;
    @ManyToMany()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @BatchSize(size = 25)
    private List<Department> departmentsObservers;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private TaskStage currentStage;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_group_id")
    private TaskGroup group;

    private Long parent;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_parent_id")
    @BatchSize(size = 25)
    private List<Task> children;

    /**
     * Создает отсортированный список полей для отображения в элементе списка
     * @return Список полей задачи
     */
    public List<ModelItem> getListItemFields(){
        return modelWireframe.getAllFields().stream()
                .filter(f->f.getListViewIndex()!=null)
                .sorted(Comparator.comparing(FieldItem::getListViewIndex))
                .map(fieldItem -> fields.stream().filter(f->f.getId().equals(fieldItem.getId())).findFirst().orElse(null))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Задача {");
        sb.append("taskId=").append(taskId);
        sb.append(", Создана=").append(created);
        sb.append(", Статус=").append(taskStatus);
        sb.append(", Шаблон=").append(modelWireframe);
        sb.append(", Поля=").append(fields);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(getTaskId(), task.getTaskId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskId());
    }

    public void setFields(List<ModelItem> fieldsAppend) {
        fields = fieldsAppend.stream().peek(f -> f.setTask(this)).collect(Collectors.toList());
    }

    public void editFields(List<ModelItem> fields) {
        if (fields == null) return;
        // Заменяем поле если оно уже существует
        this.fields.forEach(f->{
            ModelItem  newField = fields.stream().filter(ff -> ff.getModelItemId().equals(f.getModelItemId())).findFirst().orElse(null);
            if(newField == null) return;
            Object oldValue = f.getValue();
            if(oldValue != null && newField.getValue() != null && !oldValue.equals(newField.getValue())) {
                f.setValue(newField.getValue());
            }else if(oldValue == null && newField.getValue() != null) {
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
        }).collect(Collectors.toSet());
    }

    public Set<Employee> getAllEmployeesObservers(Employee exclude) {
        Set<Employee> employees = new HashSet<>(employeesObservers);
        for(Department department : departmentsObservers) {
            Set<Employee> departmentEmployees = department.getEmployees();
            if(departmentEmployees != null) employees.addAll(departmentEmployees);
        }
        if(exclude != null) employees.remove(exclude);
        return employees;
    }

    public Set<Employee> getAllEmployeesObservers() {
        return getAllEmployeesObservers(null);
    }

    public void appendEvent(TaskEvent taskEvent) {
        getTaskEvents().add(taskEvent);
    }

    /**
     * Объект для получения информации о задаче для создания
     */
    @Getter
    @Setter
    public static class CreationBody{
        private Long wireframeId;
        private List<ModelItem> fields;
        @Nullable
        private Long childId;
        @Nullable
        private Long parentId;
        @Nullable
        private String initialComment;
    }
}
