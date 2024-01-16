package com.microel.trackerbackend.storage.entities.templating;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "task_type_directory")
public class TaskTypeDirectory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskTypeDirectoryId;
    private String name;
    @Nullable
    private String description;
    @Nullable
    private Integer orderIndex;

    public void update(Form form) {
        if(form.getName() == null || form.getName().isBlank()) throw new ResponseException("Имя директории не указано");
        setName(form.getName());
        setDescription(form.getDescription());
        setOrderIndex(form.getOrderIndex());
    }

    @Data
    public static class Form{
        @Nullable
        private Long taskTypeDirectoryId;
        @Nullable
        private String name;
        @Nullable
        private String description;
        @Nullable
        private Integer orderIndex;

        public TaskTypeDirectory toEntity() {
            if(name == null || name.isBlank()) throw new ResponseException("Имя директории не указано");
            return TaskTypeDirectory.builder()
                    .taskTypeDirectoryId(taskTypeDirectoryId)
                    .name(name)
                    .description(description)
                    .orderIndex(orderIndex)
                    .build();
        }
    }
}
