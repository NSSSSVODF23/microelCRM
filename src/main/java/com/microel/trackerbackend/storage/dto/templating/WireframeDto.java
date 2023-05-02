package com.microel.trackerbackend.storage.dto.templating;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.WireframeType;
import com.microel.trackerbackend.storage.entities.templating.model.dto.StepItem;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class WireframeDto {
    private Long wireframeId;
    private WireframeType wireframeType;
    private String name;
    private String description;
    private List<StepItem> steps;
    private Set<TaskStageDto> stages;
    private List<DefaultObserverDto> defaultObservers;
    private Timestamp created;
    private Employee creator;
    private Boolean deleted;
    private String listViewType;
    private String detailedViewType;

    @JsonIgnore
    public TaskStageDto getStageByName(@Nullable String name) {
        if(name == null) return null;
        return getStages().stream().filter(stage -> stage.getLabel().equals(name)).findFirst().orElse(null);
    }
}
