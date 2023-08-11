package com.microel.trackerbackend.storage.entities.salary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.ResponseWorkEstimationForm;
import com.microel.trackerbackend.misc.WorkCalculationForm;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "actions_taken")
public class ActionTaken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionTakenId;
    @ManyToOne
    @JoinColumn(name = "f_paid_work_id")
    @Nullable
    @JsonIgnore
    private PaidWork work;
    @OneToOne
    private PaidAction paidAction;
    private Integer count;
    private UUID uuid;

    @Nullable
    public String getWorkName(){
        if(work == null){
            return null;
        }
        return work.getName();
    }

    @Nullable
    public Long getWorkId(){
        if(work == null){
            return null;
        }
        return work.getPaidWorkId();
    }

    @JsonIgnore
    public ResponseWorkEstimationForm.WorkActionFormItem toFormItem(){
        return ResponseWorkEstimationForm.WorkActionFormItem.builder()
                .workName(getWorkName())
                .workId(getWorkId())
                .actionName(paidAction.getName())
                .actionId(paidAction.getPaidActionId())
                .count(count)
                .unit(paidAction.getUnit())
                .price(paidAction.getCost())
                .cost(paidAction.getCost()*count)
                .uuid(uuid)
                .build();
    }
}
