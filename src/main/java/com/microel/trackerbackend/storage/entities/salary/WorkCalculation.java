package com.microel.trackerbackend.storage.entities.salary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.FactorAction;
import com.microel.trackerbackend.misc.ResponseWorkEstimationForm;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
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
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<EmployeeIntervention> editedBy;
    @ManyToOne
    private Employee creator;
    @Column(columnDefinition = "boolean default false")
    private Boolean empty;
    @Column(columnDefinition = "text default ''")
    private String emptyDescription;
    @Column(columnDefinition = "boolean default false")
    private Boolean isPaidWork;
    @Nullable
    private Float amountOfMoneyTaken;

    public void addEditedBy(Employee employee, String description) {
        this.editedBy.add(EmployeeIntervention.builder()
                .employee(employee)
                .description(description)
                .timestamp(Timestamp.from(Instant.now()))
                .build());
    }

    @Nullable
    public EmployeeIntervention getLastEdit() {
        if (editedBy == null) return null;
        return editedBy.stream().max(Comparator.comparing(EmployeeIntervention::getTimestamp)).orElse(null);
    }

    @JsonIgnore
    public List<ResponseWorkEstimationForm.FactorWorkActionFormItem> getFactorActionsFormItems() {
        if (factorsActions == null) {
            return List.of();
        }
        return factorsActions.stream().map(factorAction -> ResponseWorkEstimationForm.FactorWorkActionFormItem.builder()
                .factor(factorAction.getFactor())
                .actionUuids(factorAction.getActionUuids())
                .uuid(UUID.randomUUID())
                .login(employee.getLogin())
                .name(factorAction.getName())
                .build()).collect(Collectors.toList());
    }

    public float getSum(Boolean withoutFactorsActions) {
        if(isPaidWork && amountOfMoneyTaken != null && amountOfMoneyTaken > 0) {
            return 0f;
        }
        Float actionsSum = actions.stream().map(actionTaken -> actionTaken.getPaidAction().getCost() * actionTaken.getCount()).reduce(0f, Float::sum);
        float originalSum = actionsSum * ratio;
        if (factorsActions != null && !withoutFactorsActions) {
            for (FactorAction factorAction : factorsActions) {
                Float factorActionsSum = factorAction.getActionUuids().stream().map(uuid -> {
                    ActionTaken actionTaken = actions.stream().filter(action -> action.getUuid().equals(uuid)).findFirst().orElse(null);
                    if (actionTaken != null) {
                        return actionTaken.getPaidAction().getCost() * actionTaken.getCount();
                    }
                    return 0f;
                }).reduce(0f, Float::sum);
                originalSum += ((factorActionsSum * ratio) * factorAction.getFactor()) - (factorActionsSum * ratio);
            }
        }
        return originalSum;
    }

    public float getSum() {
        return getSum(false);
    }

    public float getSumWithoutNDFL() {
        if(isPaidWork && amountOfMoneyTaken != null && amountOfMoneyTaken > 0) {
            return 0f;
        }
        Float actionsSum = actions.stream().map(actionTaken -> {
            Float cost = actionTaken.getPaidAction().getCost();
            float costWithoutNDFL = cost - (cost * .13f);
            return costWithoutNDFL * actionTaken.getCount();
        }).reduce(0f, Float::sum);
        float originalSum = actionsSum * ratio;
        if (factorsActions != null) {
            for (FactorAction factorAction : factorsActions) {
                Float factorActionsSum = factorAction.getActionUuids().stream().map(uuid -> {
                    ActionTaken actionTaken = actions.stream().filter(action -> action.getUuid().equals(uuid)).findFirst().orElse(null);
                    if (actionTaken != null) {
                        return actionTaken.getPaidAction().getCost() * actionTaken.getCount();
                    }
                    return 0f;
                }).reduce(0f, Float::sum);
                originalSum += ((factorActionsSum * ratio) * factorAction.getFactor()) - (factorActionsSum * ratio);
            }
        }
        return Math.max(originalSum, 0f);
    }

    public boolean isNotEmpty(){
        return !(actions == null || actions.size()==0);
    }

    public boolean isNotZero(){
        return getSum() > 0;
    }
}
