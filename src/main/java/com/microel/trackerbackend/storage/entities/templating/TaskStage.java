package com.microel.trackerbackend.storage.entities.templating;

import com.microel.trackerbackend.storage.entities.templating.oldtracker.OldTrackerBind;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.CascadeType;
import java.util.ArrayList;
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
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_old_tracker_bind_id")
    private OldTrackerBind oldTrackerBind;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    @OrderBy(value = "orderIndex")
    private List<TaskTypeDirectory> directories;

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
            return TaskStage.builder()
                    .stageId(stageId)
                    .label(label)
                    .orderIndex(orderIndex)
                    .oldTrackerBind(oldTrackerBind == null ? null : oldTrackerBind.toEntity())
                    .directories(directories == null ? new ArrayList<>() : directories.stream().map(TaskTypeDirectory.Form::toEntity).collect(Collectors.toList()))
                    .build();
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
}
