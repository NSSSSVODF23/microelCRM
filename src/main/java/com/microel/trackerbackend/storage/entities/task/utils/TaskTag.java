package com.microel.trackerbackend.storage.entities.task.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "task_tags")
public class TaskTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskTagId;
    @Column(length = 48)
    private String name;
    @Column(length = 7, columnDefinition = "varchar(7) default '#666666'")
    private String color;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    private Timestamp created;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_creator_id")
    private Employee creator;
    @JsonIgnore
    @ManyToMany(mappedBy = "tags")
    @BatchSize(size = 25)
    private Set<Task> task;
    @Column(columnDefinition = "boolean default false")
    @Nullable
    private Boolean unbindAfterClose;

    public Boolean getUnbindAfterClose(){
        return unbindAfterClose != null && unbindAfterClose;
    }

    public SimpleTag toSimpleTag() {
        return new SimpleTag(name, color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskTag)) return false;
        TaskTag taskTag = (TaskTag) o;
        return Objects.equals(getTaskTagId(), taskTag.getTaskTagId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskTagId());
    }

    @Getter
    @Setter
    public static class Form {
        @Nullable
        private Long id;
        private String name;
        private String color;
        private Boolean unbindAfterClose;

        public void throwIfIncomplete() {
            if (name == null || name.isBlank()) throw new ResponseException("Название не установлено");
            if (color == null || color.isBlank()) throw new ResponseException("Цвет не установлен");
            if (unbindAfterClose == null) throw new ResponseException("Не установлено поведение при закрытии задачи");
        }

        public TaskTag toTaskTag() {
            return TaskTag.builder()
                    .name(name)
                    .color(color)
                    .unbindAfterClose(unbindAfterClose)
                    .build();
        }
    }

    @Data
    public static class SimpleTag {
        @NonNull
        private String name;
        @NonNull
        private String color;
    }
}
