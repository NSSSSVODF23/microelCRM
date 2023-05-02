package com.microel.trackerbackend.storage.entities.templating;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
}
