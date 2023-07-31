package com.microel.trackerbackend.storage.entities.salary;

import com.microel.trackerbackend.misc.FactorAction;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.lang.Nullable;

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
@Table(name = "work_calculations")
public class WorkCalculation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workCalculationId;
    @OneToOne
    private WorkLog workLog;
    @ManyToMany()
    @BatchSize(size = 25)
    private List<ActionTaken> actions;
    @ManyToOne
    private Employee employee;
    private Float ratio;
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    @Nullable
    private List<FactorAction> factorsActions;
    private Timestamp created;
    @ManyToOne
    private Employee creator;
    @Column(columnDefinition = "boolean default false")
    private Boolean empty;
    @Column(columnDefinition = "text default ''")
    private String emptyDescription;


    public Integer getSum(){
        Float actionsSum = actions.stream().map(actionTaken -> actionTaken.getPaidAction().getCost() * actionTaken.getCount()).reduce(0f, Float::sum);
        int originalSum = Math.round(actionsSum * ratio);
        if(factorsActions != null){
            for(FactorAction factorAction : factorsActions){
                Float factorActionsSum = factorAction.getActionUuids().stream().map(uuid -> {
                    ActionTaken actionTaken = actions.stream().filter(action -> action.getUuid().equals(uuid)).findFirst().orElse(null);
                    if(actionTaken != null){
                        return actionTaken.getPaidAction().getCost() * actionTaken.getCount();
                    }
                    return 0f;
                }).reduce(0f, Float::sum);
                originalSum += Math.round(((factorActionsSum * ratio) * factorAction.getFactor())-(factorActionsSum * ratio));
            }
        }
        return originalSum;
    }
}
