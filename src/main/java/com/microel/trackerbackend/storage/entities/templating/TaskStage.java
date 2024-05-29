package com.microel.trackerbackend.storage.entities.templating;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.OldTrackerBind;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "task_stages")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStage {
    @Id
    private String stageId;
    private String label;
    private Integer orderIndex;
    @ManyToOne
    @JoinColumn(name = "f_wireframe_id")
    @JsonIgnore
    private Wireframe wireframe;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_old_tracker_bind_id")
    private OldTrackerBind oldTrackerBind;
    @OneToMany(mappedBy = "stage", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    @OrderBy(value = "orderIndex")
    private List<TaskTypeDirectory> directories;

    public void appendDirectory(TaskTypeDirectory directory) {
        if (this.directories == null) {
            this.directories = new ArrayList<>();
        }
        this.directories.add(directory);
        directory.setStage(this);
    }

    public void setDirectories(List<TaskTypeDirectory> directories) {
        if (this.directories == null) {
            this.directories = new ArrayList<>();
            return;
        }
        for (TaskTypeDirectory directory : directories) {
            if (directory.getStage() == null) {
                directory.setStage(this);
            }
        }
        this.directories = directories;
    }

    public void clearRemovedDirectories(List<TaskTypeDirectory> removedDirectories)  {
        for (TaskTypeDirectory directory : removedDirectories) {
            if (directory.getStage() == this) {
                directory.setStage(null);
            }
            getDirectories().remove(directory);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskStage taskStage)) return false;
        return Objects.equals(getStageId(), taskStage.getStageId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStageId());
    }

    @Data
    public static class Form {
        private String stageId;
        private String label;
        private Integer orderIndex;
        @Nullable
        private OldTrackerBind.Form oldTrackerBind;
        @Nullable
        private List<TaskTypeDirectory.Form> directories;

        public TaskStage toEntity() {
            TaskStage stage = TaskStage.builder()
                    .stageId(stageId)
                    .label(label)
                    .orderIndex(orderIndex)
                    .oldTrackerBind(oldTrackerBind == null ? null : oldTrackerBind.toEntity())
                    .build();
            stage.setDirectories(directories == null ?
                    new ArrayList<>() :
                    directories.stream().map(TaskTypeDirectory.Form::toEntity).collect(Collectors.toList()));
            return stage;
        }
    }
}
