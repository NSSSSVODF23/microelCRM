package com.microel.trackerbackend.storage.entities.templating.oldtracker;

import com.microel.trackerbackend.storage.entities.templating.oldtracker.fields.FieldDataBind;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "old_tracker_binds")
public class OldTrackerBind {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oldTrackerBindId;
    private Integer classId;
    private Integer initialStageId;
    private Integer processingStageId;
    private Integer manualCloseStageId;
    private Integer autoCloseStageId;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<FieldDataBind> fieldDataBinds;

    @Data
    public static class Form {
        private Integer classId;
        private Integer initialStageId;
        private Integer processingStageId;
        private Integer manualCloseStageId;
        private Integer autoCloseStageId;
        private List<FieldDataBind.Form> fieldDataBinds;

        public OldTrackerBind toEntity() {
            OldTrackerBind oldTrackerBind = new OldTrackerBind();
            oldTrackerBind.setClassId(classId);
            oldTrackerBind.setInitialStageId(initialStageId);
            oldTrackerBind.setProcessingStageId(processingStageId);
            oldTrackerBind.setManualCloseStageId(manualCloseStageId);
            oldTrackerBind.setAutoCloseStageId(autoCloseStageId);
            oldTrackerBind.setFieldDataBinds(fieldDataBinds.stream().map(FieldDataBind.Form::toEntity).collect(Collectors.toList()));
            return oldTrackerBind;
        }
    }
}
