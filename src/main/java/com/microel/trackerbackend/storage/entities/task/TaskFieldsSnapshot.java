package com.microel.trackerbackend.storage.entities.task;

import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "json", typeClass = JsonType.class)
@Builder
@Table(name = "task_fields_snapshots")
public class TaskFieldsSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskFieldsSnapshotId;
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private List<ModelItem> beforeEditing;
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private List<ModelItem> afterEditing;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_task_id")
    private Task task;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_employee_id")
    private Employee whoEdited;
    private Timestamp whenEdited;
}
