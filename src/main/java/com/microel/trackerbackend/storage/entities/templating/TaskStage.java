package com.microel.trackerbackend.storage.entities.templating;

import com.microel.trackerbackend.storage.entities.templating.oldtracker.OldTrackerBind;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.CascadeType;

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

    @Data
    public static class Form {
        private String stageId;
        private String label;
        private Integer orderIndex;
        private OldTrackerBind.Form oldTrackerBind;

        public TaskStage toEntity() {
            return TaskStage.builder()
                    .stageId(stageId)
                    .label(label)
                    .orderIndex(orderIndex)
                    .oldTrackerBind(oldTrackerBind.toEntity())
                    .build();
        }
    }
}
